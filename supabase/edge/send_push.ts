// ============================================================================
// Smart Teacher (المعلم الذكي) - Supabase Edge Function: send_push
// ============================================================================
// Sends Firebase Cloud Messaging (FCM) push notifications to all students in
// a given grade + section.
//
// Deploy with the Supabase CLI:
//   supabase functions deploy send_push --no-verify-jwt
//
// Set this secret (Supabase Dashboard → Edge Functions → Secrets):
//   FCM_SERVER_KEY  = your Firebase Cloud Messaging Server Key (legacy)
//   -- OR --
//   FCM_SERVICE_ACCOUNT_JSON = the full service-account JSON for v1 API
//
// The Android app calls this function via NotificationTrigger.kt with a POST
// body like:
//   {
//     "grade": "الصف السادس",
//     "section": "أ",
//     "type": "assignment" | "exam" | "note",
//     "title": "...",
//     "body": "..."
//   }
// ============================================================================

const FCM_LEGACY_URL = "https://fcm.googleapis.com/fcm/send";

// CORS headers
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  // Handle CORS pre-flight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { grade, section, type, title, body } = await req.json();

    if (!grade || !section || !title) {
      return new Response(
        JSON.stringify({ error: "grade, section and title are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    // 1. Look up all FCM tokens for the class/section using the service-role
    //    key so we bypass RLS.
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    const tokensResp = await fetch(
      `${supabaseUrl}/rest/v1/rpc/get_class_tokens`,
      {
        method: "POST",
        headers: {
          "apikey": serviceRoleKey,
          "Authorization": `Bearer ${serviceRoleKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ p_grade: grade, p_section: section }),
      },
    );

    const tokensData = await tokensResp.json();
    const tokens: string[] = Array.isArray(tokensData)
      ? tokensData.map((t: any) => t.fcm_token).filter(Boolean)
      : [];

    if (tokens.length === 0) {
      return new Response(
        JSON.stringify({ sent: 0, message: "no registered tokens for this class" }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    // 2. Send the push via FCM legacy HTTP API
    const serverKey = Deno.env.get("FCM_SERVER_KEY");

    if (!serverKey) {
      console.warn("FCM_SERVER_KEY secret not set — skipping actual send.");
      return new Response(
        JSON.stringify({
          sent: 0,
          tokens: tokens.length,
          error: "FCM_SERVER_KEY not configured",
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const notification = {
      title: title,
      body: body ?? "",
      sound: "default",
    };

    const data = {
      type: type ?? "general",
      grade: grade,
      section: section,
      click_action: "FLUTTER_NOTIFICATION_CLICK",
    };

    const fcmResp = await fetch(FCM_LEGACY_URL, {
      method: "POST",
      headers: {
        "Authorization": `key=${serverKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        registration_ids: tokens,
        notification,
        data,
        priority: "high",
      }),
    });

    const fcmResult = await fcmResp.json();

    return new Response(
      JSON.stringify({
        sent: tokens.length,
        success: fcmResult.success ?? 0,
        failure: fcmResult.failure ?? 0,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    console.error("send_push error:", err);
    return new Response(
      JSON.stringify({ error: String(err) }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
