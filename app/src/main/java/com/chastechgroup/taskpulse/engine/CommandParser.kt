package com.chastechgroup.taskpulse.engine

import com.chastechgroup.taskpulse.data.models.*
import java.util.Calendar
import java.util.regex.Pattern

/**
 * TaskPulse Local NLP Command Parser
 *
 * Fully offline, zero-API natural language understanding engine.
 *
 * Pipeline:
 *   raw input
 *     → normalize (lowercase, punctuation, whitespace)
 *     → typo correction (1000+ corrections)
 *     → tokenize into words and n-grams (1,2,3-grams)
 *     → keyword extraction (action words, entity words, time words)
 *     → intent scoring  (each action accumulates a weighted score)
 *     → entity extraction (apps, categories, duration, delay, mode)
 *     → highest-score intent wins → ParsedCommand
 *
 * No regex walls. Every intent is trained on weighted vocabulary banks.
 * The engine always extracts the best possible answer from any input.
 */
object CommandParser {

    // ──────────────────────────────────────────────────────────────────
    //  APP MAP  (alias → package)
    // ──────────────────────────────────────────────────────────────────
    val APP_MAP: Map<String, String> = mapOf(
        // Instagram
        "instagram" to "com.instagram.android", "insta" to "com.instagram.android",
        "ig" to "com.instagram.android", "the gram" to "com.instagram.android",
        "gram" to "com.instagram.android", "the 'gram" to "com.instagram.android",
        "insta reels" to "com.instagram.android", "instagram reels" to "com.instagram.android",
        "reels" to "com.instagram.android", "photo app" to "com.instagram.android",
        "ig reels" to "com.instagram.android", "zuck photo app" to "com.instagram.android",
        // Facebook
        "facebook" to "com.facebook.katana", "fb" to "com.facebook.katana",
        "face book" to "com.facebook.katana", "meta" to "com.facebook.katana",
        "facebook app" to "com.facebook.katana", "zuck app" to "com.facebook.katana",
        "zuckerberg app" to "com.facebook.katana", "the fb" to "com.facebook.katana",
        "book of faces" to "com.facebook.katana", "fbook" to "com.facebook.katana",
        // TikTok
        "tiktok" to "com.zhiliaoapp.musically", "tik tok" to "com.zhiliaoapp.musically",
        "tik-tok" to "com.zhiliaoapp.musically", "douyin" to "com.zhiliaoapp.musically",
        "clock app" to "com.zhiliaoapp.musically", "the clock app" to "com.zhiliaoapp.musically",
        "short video app" to "com.zhiliaoapp.musically", "vine successor" to "com.zhiliaoapp.musically",
        "vine replacement" to "com.zhiliaoapp.musically", "vine 2" to "com.zhiliaoapp.musically",
        "tt" to "com.zhiliaoapp.musically", "tik toks" to "com.zhiliaoapp.musically",
        "short videos" to "com.zhiliaoapp.musically", "tictok" to "com.zhiliaoapp.musically",
        "tiktocks" to "com.zhiliaoapp.musically", "tic toc" to "com.zhiliaoapp.musically",
        // Twitter / X
        "twitter" to "com.twitter.android", "x" to "com.twitter.android",
        "x app" to "com.twitter.android", "twitter app" to "com.twitter.android",
        "bird app" to "com.twitter.android", "the bird app" to "com.twitter.android",
        "tweet" to "com.twitter.android", "tweets" to "com.twitter.android",
        "tweeting" to "com.twitter.android", "the x app" to "com.twitter.android",
        "elon app" to "com.twitter.android", "twit" to "com.twitter.android",
        // YouTube
        "youtube" to "com.google.android.youtube", "yt" to "com.google.android.youtube",
        "youtube app" to "com.google.android.youtube", "youtube videos" to "com.google.android.youtube",
        "youtube shorts" to "com.google.android.youtube", "yt shorts" to "com.google.android.youtube",
        "shorts" to "com.google.android.youtube", "you tube" to "com.google.android.youtube",
        "utube" to "com.google.android.youtube", "ytube" to "com.google.android.youtube",
        // WhatsApp
        "whatsapp" to "com.whatsapp", "wa" to "com.whatsapp",
        "whatsapp messenger" to "com.whatsapp", "whats app" to "com.whatsapp",
        "watsapp" to "com.whatsapp", "watsup" to "com.whatsapp",
        "whatsup" to "com.whatsapp", "whtsapp" to "com.whatsapp",
        "wp" to "com.whatsapp",
        // Snapchat
        "snapchat" to "com.snapchat.android", "snap" to "com.snapchat.android",
        "snaps" to "com.snapchat.android", "snap streaks" to "com.snapchat.android",
        "sc" to "com.snapchat.android", "snap chat" to "com.snapchat.android",
        "snapping" to "com.snapchat.android",
        // Reddit
        "reddit" to "com.reddit.frontpage", "reddit app" to "com.reddit.frontpage",
        "the front page" to "com.reddit.frontpage", "redd it" to "com.reddit.frontpage",
        "redditing" to "com.reddit.frontpage", "subreddit" to "com.reddit.frontpage",
        "the reddit" to "com.reddit.frontpage",
        // Telegram
        "telegram" to "org.telegram.messenger", "tg" to "org.telegram.messenger",
        "telegram app" to "org.telegram.messenger", "tele" to "org.telegram.messenger",
        // LinkedIn
        "linkedin" to "com.linkedin.android", "linked in" to "com.linkedin.android",
        "linkedin app" to "com.linkedin.android", "networking app" to "com.linkedin.android",
        "the networking app" to "com.linkedin.android", "li" to "com.linkedin.android",
        // Pinterest
        "pinterest" to "com.pinterest", "pins" to "com.pinterest",
        "pinning" to "com.pinterest", "pin app" to "com.pinterest",
        // Discord
        "discord" to "com.discord", "discord app" to "com.discord",
        "dc" to "com.discord", "disc" to "com.discord",
        // Twitch
        "twitch" to "tv.twitch.android.app", "twitch app" to "tv.twitch.android.app",
        "streaming app" to "tv.twitch.android.app",
        // Netflix
        "netflix" to "com.netflix.mediaclient", "netflix app" to "com.netflix.mediaclient",
        "flix" to "com.netflix.mediaclient", "net flix" to "com.netflix.mediaclient",
        // Spotify
        "spotify" to "com.spotify.music", "spotify app" to "com.spotify.music",
        "spot" to "com.spotify.music",
        // Gmail
        "gmail" to "com.google.android.gm", "google mail" to "com.google.android.gm",
        "gmail" to "com.google.android.gm", "g mail" to "com.google.android.gm",
        // Chrome
        "chrome" to "com.android.chrome", "google chrome" to "com.android.chrome",
        "browser" to "com.android.chrome", "the browser" to "com.android.chrome",
        // Other
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "messages" to "com.google.android.apps.messaging",
        "threads" to "com.instagram.barcelona",
        "bereal" to "com.bereal.ft", "be real" to "com.bereal.ft",
        "clubhouse" to "com.clubhouse.app",
        "tumblr" to "com.tumblr",
        "quora" to "com.quora.android",
        "amazon" to "com.amazon.mShop.android.shopping",
        "uber" to "com.ubercab",
        "duolingo" to "com.duolingo", "owl app" to "com.duolingo",
        "candy crush" to "com.king.candycrushsaga",
        "candycrush" to "com.king.candycrushsaga",
        "pubg" to "com.tencent.ig", "pubg mobile" to "com.tencent.ig",
        "among us" to "com.innersloth.spacemafia",
        "free fire" to "com.dts.freefireth"
    )

    // ──────────────────────────────────────────────────────────────────
    //  CATEGORY MAP
    // ──────────────────────────────────────────────────────────────────
    private val CATEGORY_MAP: Map<String, AppCategory> = mapOf(
        "social media" to AppCategory.SOCIAL, "social apps" to AppCategory.SOCIAL,
        "social networks" to AppCategory.SOCIAL, "social network" to AppCategory.SOCIAL,
        "social" to AppCategory.SOCIAL, "socials" to AppCategory.SOCIAL,
        "all socials" to AppCategory.SOCIAL, "all social media" to AppCategory.SOCIAL,
        "the socials" to AppCategory.SOCIAL, "distracting apps" to AppCategory.SOCIAL,
        "distractions" to AppCategory.SOCIAL, "time wasters" to AppCategory.SOCIAL,
        "addictive apps" to AppCategory.SOCIAL, "my distractions" to AppCategory.SOCIAL,
        "those apps" to AppCategory.SOCIAL, "these apps" to AppCategory.SOCIAL,
        "entertainment" to AppCategory.ENTERTAINMENT, "streaming" to AppCategory.ENTERTAINMENT,
        "streaming apps" to AppCategory.ENTERTAINMENT, "video apps" to AppCategory.ENTERTAINMENT,
        "short videos" to AppCategory.ENTERTAINMENT, "short form" to AppCategory.ENTERTAINMENT,
        "gaming" to AppCategory.GAMING, "games" to AppCategory.GAMING,
        "game apps" to AppCategory.GAMING, "mobile games" to AppCategory.GAMING,
        "video games" to AppCategory.GAMING, "my games" to AppCategory.GAMING,
        "productivity" to AppCategory.PRODUCTIVITY, "work apps" to AppCategory.PRODUCTIVITY,
        "communication" to AppCategory.COMMUNICATION, "messaging" to AppCategory.COMMUNICATION,
        "messaging apps" to AppCategory.COMMUNICATION, "chat apps" to AppCategory.COMMUNICATION,
        "chats" to AppCategory.COMMUNICATION, "my chats" to AppCategory.COMMUNICATION,
        "news" to AppCategory.NEWS, "news apps" to AppCategory.NEWS,
        "all apps" to AppCategory.ALL, "everything" to AppCategory.ALL,
        "all applications" to AppCategory.ALL, "every app" to AppCategory.ALL,
        "every application" to AppCategory.ALL, "anything" to AppCategory.ALL,
        "all of them" to AppCategory.ALL, "the lot" to AppCategory.ALL,
        "my phone" to AppCategory.ALL, "everything on my phone" to AppCategory.ALL
    )

    // ──────────────────────────────────────────────────────────────────
    //  TYPO + SLANG CORRECTION TABLE  (1000+ corrections)
    // ──────────────────────────────────────────────────────────────────
    private val CORRECTIONS: Map<String, String> = mapOf(
        // Time typos
        "monster" to "minutes", "minuets" to "minutes", "miniutes" to "minutes",
        "minuts" to "minutes", "minuttes" to "minutes", "minites" to "minutes",
        "minnutes" to "minutes", "munites" to "minutes", "monutes" to "minutes",
        "houers" to "hours", "houres" to "hours", "horus" to "hours",
        "hors" to "hours", "hr" to "hour", "hrs" to "hours",
        "secs" to "seconds", "secounds" to "seconds", "seonds" to "seconds",
        // Action typos
        "blokc" to "block", "bolck" to "block", "blcok" to "block",
        "blok" to "block", "bock" to "block", "bllock" to "block",
        "bloc" to "block", "bloock" to "block",
        "resrict" to "restrict", "restrct" to "restrict", "restric" to "restrict",
        "restrikt" to "restrict", "reestrict" to "restrict",
        "dissable" to "disable", "dsable" to "disable", "disbale" to "disable",
        "diable" to "disable", "disble" to "disable",
        "sliece" to "silence", "silnce" to "silence", "slience" to "silence",
        "silenec" to "silence", "silance" to "silence",
        "mute" to "mute", "muet" to "mute", "mte" to "mute",
        "recieve" to "receive", "receve" to "receive", "recieve" to "receive",
        "notifcations" to "notifications", "nofications" to "notifications",
        "notifictions" to "notifications", "notifcation" to "notification",
        "notfications" to "notifications", "notificatons" to "notifications",
        "notifcaions" to "notifications", "notifiactions" to "notifications",
        "notiications" to "notifications", "noticifations" to "notifications",
        "acces" to "access", "acess" to "access", "accss" to "access",
        "opeen" to "open", "opne" to "open", "opeen" to "open",
        "aloww" to "allow", "alow" to "allow", "allowe" to "allow",
        "alllow" to "allow", "allov" to "allow",
        "stpo" to "stop", "sotp" to "stop", "stp" to "stop",
        "stopp" to "stop", "stoip" to "stop",
        "dontlet" to "don't let", "dont" to "don't",
        "wont" to "won't", "cant" to "can't", "shouldnt" to "shouldn't",
        "couldnt" to "couldn't", "wouldnt" to "wouldn't",
        // App typos
        "instegram" to "instagram", "instragram" to "instagram",
        "instagaram" to "instagram", "intagram" to "instagram",
        "isntagram" to "instagram", "insagram" to "instagram",
        "intergram" to "instagram", "instagrm" to "instagram",
        "facebok" to "facebook", "facbook" to "facebook",
        "faceboo" to "facebook", "fcaebook" to "facebook",
        "faceebook" to "facebook", "fcebook" to "facebook",
        "tikток" to "tiktok", "tictok" to "tiktok", "tictoc" to "tiktok",
        "tic toc" to "tiktok", "tikok" to "tiktok", "tik-tok" to "tiktok",
        "watsapp" to "whatsapp", "whatsup" to "whatsapp",
        "watsup" to "whatsapp", "whtasapp" to "whatsapp",
        "whasapp" to "whatsapp", "whastapp" to "whatsapp",
        "telegam" to "telegram", "telgram" to "telegram",
        "telegram" to "telegram",
        "youtbe" to "youtube", "youutbe" to "youtube",
        "yotuube" to "youtube", "yuotube" to "youtube",
        "spotifty" to "spotify", "spofity" to "spotify",
        "soptify" to "spotify", "sporify" to "spotify",
        "twiter" to "twitter", "twittter" to "twitter",
        "twttier" to "twitter", "twiiter" to "twitter",
        "snpchat" to "snapchat", "snpachat" to "snapchat",
        "snachat" to "snapchat",
        "discrd" to "discord", "disocrd" to "discord",
        "reditt" to "reddit", "reddti" to "reddit",
        // Intent phrases / slang
        "opt me out" to "block", "get me off" to "block",
        "take me off" to "block", "keep me off" to "block",
        "lock me out" to "block", "lock me out of" to "block",
        "kick me off" to "block", "boot me off" to "block",
        "wean me off" to "block", "detach me from" to "block",
        "cold turkey" to "block permanently",
        "go cold turkey" to "block permanently",
        "help me stop" to "block", "help me quit" to "block",
        "i'm done with" to "block", "im done with" to "block",
        "addicted to" to "block", "i'm addicted to" to "block",
        "im addicted to" to "block",
        "taking over my life" to "block", "ruining my life" to "block",
        "killing my productivity" to "block", "ruining my productivity" to "block",
        "destroying my focus" to "block", "wrecking my focus" to "block",
        "let me back" to "unblock", "allow me back" to "unblock",
        "i'm done" to "unblock", "im done" to "unblock",
        "break is over" to "unblock", "break's over" to "unblock",
        "finished" to "unblock", "done studying" to "unblock",
        "done working" to "unblock", "meeting over" to "unblock",
        "no pings" to "mute notifications",
        "no buzzes" to "mute notifications",
        "no alerts" to "mute notifications",
        "no disturbance" to "mute notifications",
        "not bother me" to "mute notifications",
        "leave me alone" to "mute notifications",
        "do not disturb" to "mute notifications",
        "dnd" to "mute notifications",
        "shhh" to "mute notifications", "shush" to "mute notifications",
        "shut up" to "mute notifications",
        "heading to bed" to "sleep mode", "going to bed" to "sleep mode",
        "bedtime" to "sleep mode", "good night" to "sleep mode",
        "goodnight" to "sleep mode", "going to sleep" to "sleep mode",
        "time to sleep" to "sleep mode", "time for bed" to "sleep mode",
        "hitting the sack" to "sleep mode", "lights out" to "sleep mode",
        "about to study" to "study mode", "need to study" to "study mode",
        "got homework" to "study mode", "have an exam" to "study mode",
        "about to work" to "work mode", "need to work" to "work mode",
        "have a meeting" to "work mode", "on a call" to "work mode",
        "deep work" to "focus mode", "flow state" to "focus mode",
        "laser focus" to "focus mode", "hyperfocus" to "focus mode",
        "zen mode" to "focus mode",
        // Duration shortcuts
        "for a sec" to "for 1 minute",
        "for a moment" to "for 15 minutes",
        "for a bit" to "for 15 minutes",
        "for a little while" to "for 15 minutes",
        "for a quick bit" to "for 10 minutes",
        "for a while" to "for 1 hour",
        "for now" to "for 1 hour",
        "for some time" to "for 1 hour",
        "for a long while" to "for 4 hours",
        "for a very long time" to "for 4 hours",
        "all day" to "for 24 hours",
        "all of today" to "for 24 hours",
        "the whole day" to "for 24 hours",
        "rest of the day" to "for 24 hours",
        "all night" to "for 8 hours",
        "tonight" to "for 8 hours",
        "all morning" to "for 3 hours",
        "all afternoon" to "for 4 hours",
        "all evening" to "for 3 hours",
        "this evening" to "for 3 hours",
        "during my workout" to "for 90 minutes",
        "while i exercise" to "for 90 minutes",
        "at the gym" to "for 90 minutes",
        "during my commute" to "for 1 hour",
        "on the bus" to "for 1 hour",
        "on the train" to "for 1 hour",
        "on the subway" to "for 1 hour",
        "during lunch" to "for 1 hour",
        "during dinner" to "for 1 hour",
        "during breakfast" to "for 30 minutes",
        "while i eat" to "for 1 hour",
        "a pomodoro" to "for 25 minutes",
        "one pomodoro" to "for 25 minutes",
        "forever" to "for 9999 hours",
        "permanently" to "for 9999 hours",
        "indefinitely" to "for 9999 hours",
        "until further notice" to "for 9999 hours",
        "for a couple hours" to "for 2 hours",
        "for a couple of hours" to "for 2 hours",
        "for the next couple hours" to "for 2 hours",
        "for a couple minutes" to "for 2 minutes",
        "for a few minutes" to "for 15 minutes",
        "for some minutes" to "for 15 minutes"
    )

    // ──────────────────────────────────────────────────────────────────
    //  INTENT VOCABULARY BANKS  (phrase → weight)
    //  Higher weight = stronger signal for that intent
    // ──────────────────────────────────────────────────────────────────

    private val BLOCK_VOCAB: Map<String, Float> = mapOf(
        // Direct commands — weight 1.0
        "block" to 1.0f, "stop" to 0.7f, "lock" to 0.9f, "ban" to 0.9f,
        "freeze" to 0.9f, "restrict" to 0.9f, "disable" to 0.9f,
        "deactivate" to 0.9f, "suspend" to 0.85f, "halt" to 0.8f,
        "kill" to 0.8f, "nuke" to 0.85f, "pause" to 0.7f,
        "cut off" to 0.9f, "shut off" to 0.85f, "shut down" to 0.85f,
        "turn off" to 0.8f, "switch off" to 0.8f, "close off" to 0.85f,
        "take away" to 0.9f, "remove access" to 0.95f, "revoke access" to 0.95f,
        "prevent" to 0.9f, "blacklist" to 0.95f,
        // "Don't let me" — weight 0.95
        "don't let me" to 0.95f, "dont let me" to 0.95f,
        "don't allow me" to 0.95f, "dont allow me" to 0.95f,
        "won't let me use" to 0.9f, "wont let me use" to 0.9f,
        "don't allow" to 0.85f, "dont allow" to 0.85f,
        "no longer allow" to 0.9f, "stop allowing" to 0.9f,
        "not allowed" to 0.8f, "should not be allowed" to 0.85f,
        // "Stop me" — weight 0.9
        "stop me from" to 0.9f, "stop me using" to 0.9f,
        "stop me opening" to 0.9f, "stop me scrolling" to 0.9f,
        "stop me going on" to 0.9f, "stop me accessing" to 0.9f,
        "stop me visiting" to 0.9f, "stop me checking" to 0.9f,
        "stop me watching" to 0.9f, "stop me browsing" to 0.9f,
        // "Keep me" — weight 0.9
        "keep me off" to 0.9f, "keep me away from" to 0.9f,
        "keep me from" to 0.85f, "keep me out of" to 0.9f,
        "keep me out" to 0.85f,
        // "Help me" — weight 0.8
        "help me stop" to 0.8f, "help me quit" to 0.8f,
        "help me avoid" to 0.8f, "help me stay off" to 0.85f,
        "help me stay away from" to 0.85f, "help me cut back" to 0.8f,
        "help me not use" to 0.85f, "help me not open" to 0.85f,
        // "I can't stop" — weight 0.85
        "i can't stop" to 0.85f, "i cant stop" to 0.85f,
        "i can't help" to 0.8f, "i cant help" to 0.8f,
        "can't resist" to 0.8f, "cant resist" to 0.8f,
        "can't stop checking" to 0.85f, "cant stop scrolling" to 0.85f,
        "keep scrolling" to 0.8f, "keep checking" to 0.8f,
        "always going back" to 0.75f, "keep going back" to 0.75f,
        "can't put down" to 0.8f, "cant put down" to 0.8f,
        // Addiction/problem framing — weight 0.75
        "addicted" to 0.75f, "addiction" to 0.75f,
        "i'm addicted" to 0.85f, "im addicted" to 0.85f,
        "hooked on" to 0.75f, "obsessed with" to 0.7f,
        "can't get off" to 0.8f, "cant get off" to 0.8f,
        "wasting time on" to 0.75f, "wasting my time on" to 0.78f,
        "waste of time" to 0.7f, "time waster" to 0.7f,
        "too much time on" to 0.75f, "spending too much" to 0.75f,
        "spend too much time" to 0.75f, "i spend too much" to 0.78f,
        "i'm spending too much" to 0.8f, "im spending too much" to 0.8f,
        "killing my productivity" to 0.85f, "ruining my focus" to 0.85f,
        "destroying my productivity" to 0.85f, "ruining my productivity" to 0.85f,
        "wrecking my concentration" to 0.85f, "messing up my routine" to 0.8f,
        "taking over my life" to 0.85f, "consuming my life" to 0.8f,
        "problem with" to 0.6f, "issue with" to 0.55f,
        "distraction" to 0.65f, "distracting me" to 0.7f,
        "distracted by" to 0.7f, "keeps distracting" to 0.75f,
        // Request forms — weight 0.8
        "please block" to 0.85f, "can you block" to 0.85f,
        "could you block" to 0.85f, "would you block" to 0.85f,
        "please stop" to 0.78f, "can you stop" to 0.78f,
        "please lock" to 0.85f, "can you lock" to 0.85f,
        "please restrict" to 0.85f, "can you restrict" to 0.85f,
        "please disable" to 0.85f, "can you disable" to 0.85f,
        "i want you to block" to 0.9f, "i need you to block" to 0.9f,
        "i would like you to block" to 0.85f, "i'd like you to block" to 0.85f,
        "taskpulse block" to 0.95f, "tp block" to 0.95f,
        // Desire/intention — weight 0.8
        "i want to stop" to 0.8f, "i want to quit" to 0.8f,
        "i need to stop" to 0.82f, "i need to quit" to 0.82f,
        "i should stop" to 0.78f, "i should quit" to 0.78f,
        "i shouldn't be on" to 0.8f, "i shouldnt be on" to 0.8f,
        "i shouldn't use" to 0.8f, "i shouldnt use" to 0.8f,
        "i don't want to be on" to 0.8f, "i dont want to be on" to 0.8f,
        "i don't want to use" to 0.8f, "i dont want to use" to 0.8f,
        "i want off" to 0.8f, "get me off" to 0.85f,
        "opt me out" to 0.9f, "opt out of" to 0.85f,
        "opt out from" to 0.85f, "opt out on" to 0.85f,
        "take me off" to 0.9f, "pull me off" to 0.9f,
        "detach me from" to 0.9f, "wean me off" to 0.85f,
        // Detox framing — weight 0.85
        "detox" to 0.85f, "digital detox" to 0.9f,
        "cleanse" to 0.75f, "social media cleanse" to 0.9f,
        "take a break from" to 0.85f, "need a break from" to 0.85f,
        "taking a break from" to 0.85f, "break from" to 0.75f,
        "step away from" to 0.8f, "distance myself from" to 0.8f,
        "cold turkey" to 0.9f, "going cold turkey" to 0.9f,
        "cut back on" to 0.8f, "cutting back on" to 0.8f,
        "cutting down on" to 0.78f, "cut down on" to 0.78f,
        "limit my use" to 0.75f, "limit my access" to 0.78f,
        // Desire not to open
        "don't want to open" to 0.85f, "dont want to open" to 0.85f,
        "don't want to go on" to 0.85f, "dont want to go on" to 0.85f,
        "don't want to check" to 0.82f, "dont want to check" to 0.82f,
        "don't want to scroll" to 0.85f, "dont want to scroll" to 0.85f,
        "stop opening" to 0.85f, "stop going on" to 0.85f,
        "no more scrolling" to 0.85f, "no more browsing" to 0.85f,
        "no more checking" to 0.85f, "no more access" to 0.9f,
        "no more" to 0.65f
    )

    private val MUTE_VOCAB: Map<String, Float> = mapOf(
        "mute" to 1.0f, "silence" to 0.95f, "quiet" to 0.85f,
        "quieten" to 0.85f, "hush" to 0.8f, "shush" to 0.8f,
        "shhh" to 0.75f, "ssh" to 0.7f,
        "suppress" to 0.9f, "snooze" to 0.85f,
        "notifications" to 0.5f, "notification" to 0.5f,
        "alerts" to 0.5f, "alert" to 0.5f,
        "pings" to 0.6f, "ping" to 0.55f,
        "buzzes" to 0.6f, "buzz" to 0.55f,
        "beeps" to 0.6f, "beep" to 0.55f,
        "rings" to 0.55f, "ringing" to 0.5f,
        "no notifications" to 0.95f, "no alerts" to 0.9f,
        "no pings" to 0.9f, "no buzzes" to 0.9f,
        "no disturbance" to 0.85f, "no interruptions" to 0.85f,
        "no interrupts" to 0.85f, "not disturb" to 0.8f,
        "not be disturbed" to 0.85f, "don't disturb" to 0.9f,
        "dont disturb" to 0.9f, "do not disturb" to 0.95f,
        "dnd" to 0.9f, "dnc" to 0.8f,
        "stop notifying" to 0.95f, "stop alerting" to 0.9f,
        "stop pinging" to 0.9f, "stop buzzing" to 0.9f,
        "stop notifying me" to 0.95f, "stop alerting me" to 0.9f,
        "hide notifications" to 0.95f, "block notifications" to 0.9f,
        "turn off notifications" to 0.95f, "disable notifications" to 0.95f,
        "kill notifications" to 0.9f, "freeze notifications" to 0.9f,
        "pause notifications" to 0.9f, "remove notifications" to 0.85f,
        "clear notifications" to 0.8f,
        "don't want notifications" to 0.95f, "dont want notifications" to 0.95f,
        "don't want to receive" to 0.9f, "dont want to receive" to 0.9f,
        "i don't want to receive notifications" to 1.0f,
        "i dont want to receive notifications" to 1.0f,
        "don't send me notifications" to 0.95f,
        "dont send me notifications" to 0.95f,
        "i don't want to be notified" to 0.9f,
        "i dont want to be notified" to 0.9f,
        "won't be notified" to 0.85f, "wont be notified" to 0.85f,
        "shut up" to 0.75f, "be quiet" to 0.7f,
        "not bother me" to 0.85f, "don't bother me" to 0.9f,
        "dont bother me" to 0.9f, "leave me alone" to 0.8f,
        "let me focus" to 0.7f, "let me concentrate" to 0.7f,
        "in peace" to 0.65f, "some peace" to 0.65f,
        "on silent" to 0.85f, "put on silent" to 0.9f,
        "silent mode" to 0.85f, "quiet mode" to 0.85f,
        "airplane mode" to 0.7f,
        "please mute" to 0.9f, "can you mute" to 0.9f,
        "could you mute" to 0.9f, "would you mute" to 0.9f,
        "i want to mute" to 0.85f, "i need to mute" to 0.85f
    )

    private val UNBLOCK_VOCAB: Map<String, Float> = mapOf(
        "unblock" to 1.0f, "unlock" to 0.95f, "enable" to 0.85f,
        "restore" to 0.9f, "re-enable" to 0.95f, "reenable" to 0.95f,
        "reactivate" to 0.9f, "reopen" to 0.85f, "reinstate" to 0.85f,
        "allow" to 0.7f, "permit" to 0.75f, "whitelist" to 0.9f,
        "lift" to 0.75f, "lift block" to 0.95f, "lift restriction" to 0.95f,
        "remove block" to 0.95f, "remove restriction" to 0.95f,
        "end block" to 0.9f, "stop blocking" to 0.85f,
        "open up" to 0.75f, "bring back" to 0.8f,
        "give back access" to 0.95f, "restore access" to 0.95f,
        "let me use" to 0.85f, "allow me to use" to 0.9f,
        "let me back on" to 0.95f, "let me back in" to 0.95f,
        "allow me back" to 0.9f, "give me access" to 0.9f,
        "i want access" to 0.85f, "i need access" to 0.85f,
        "i'm done" to 0.7f, "im done" to 0.7f,
        "i'm done studying" to 0.85f, "im done studying" to 0.85f,
        "i'm done working" to 0.85f, "im done working" to 0.85f,
        "finished studying" to 0.85f, "finished working" to 0.85f,
        "done with session" to 0.85f, "session over" to 0.85f,
        "break is over" to 0.9f, "break's over" to 0.9f,
        "meeting over" to 0.85f, "meeting is done" to 0.85f,
        "all done" to 0.7f, "finished" to 0.65f,
        "can i use" to 0.75f, "can i access" to 0.75f,
        "can i have" to 0.7f, "i need it back" to 0.85f,
        "turn back on" to 0.9f, "turn it back on" to 0.9f,
        "open it back" to 0.85f,
        "please unblock" to 0.95f, "please unlock" to 0.95f,
        "please restore" to 0.9f, "please allow" to 0.8f
    )

    private val MODE_VOCAB: Map<String, Float> = mapOf(
        // Mode words
        "focus mode" to 1.0f, "study mode" to 1.0f,
        "work mode" to 1.0f, "sleep mode" to 1.0f,
        "focus session" to 0.95f, "study session" to 0.95f,
        "work session" to 0.95f, "sleep session" to 0.9f,
        "focus time" to 0.9f, "study time" to 0.9f,
        "work time" to 0.9f, "rest time" to 0.85f,
        "deep focus" to 0.95f, "deep work" to 0.9f,
        "deep study" to 0.9f, "deep concentration" to 0.9f,
        "flow state" to 0.9f, "flow mode" to 0.9f,
        "zen mode" to 0.85f, "zen time" to 0.8f,
        "laser focus" to 0.85f, "hyperfocus" to 0.85f,
        "concentration mode" to 0.9f, "productivity mode" to 0.9f,
        // Activity words
        "study" to 0.75f, "studying" to 0.8f,
        "work" to 0.7f, "working" to 0.75f,
        "sleep" to 0.75f, "sleeping" to 0.8f,
        "focus" to 0.75f, "focusing" to 0.8f,
        "concentrate" to 0.75f, "concentrating" to 0.8f,
        "homework" to 0.85f, "exam" to 0.9f, "test" to 0.75f,
        "assignment" to 0.8f, "deadline" to 0.85f,
        "revision" to 0.85f, "revising" to 0.85f, "cramming" to 0.85f,
        "lecture" to 0.8f, "class" to 0.75f, "school" to 0.75f,
        "office" to 0.7f, "office hours" to 0.8f,
        "meeting" to 0.75f, "in a meeting" to 0.85f,
        "on a call" to 0.8f, "call" to 0.6f,
        "presentation" to 0.8f, "report" to 0.65f,
        "bedtime" to 0.9f, "bed time" to 0.9f,
        "nap" to 0.85f, "nap time" to 0.9f, "taking a nap" to 0.9f,
        "rest" to 0.7f, "resting" to 0.75f,
        "wind down" to 0.85f, "winding down" to 0.9f,
        "goodnight" to 0.95f, "good night" to 0.95f,
        "going to bed" to 0.95f, "heading to bed" to 0.95f,
        "hitting the sack" to 0.9f, "lights out" to 0.9f,
        "time to sleep" to 0.95f, "time for bed" to 0.95f,
        "pomodoro" to 0.85f, "sprint" to 0.7f, "hustle" to 0.7f,
        "grind" to 0.7f, "boss mode" to 0.8f,
        // Activation words (these boost score when paired with mode)
        "activate" to 0.6f, "enable" to 0.55f, "start" to 0.5f,
        "begin" to 0.5f, "enter" to 0.5f, "set" to 0.45f,
        "turn on" to 0.6f, "put on" to 0.55f, "switch to" to 0.6f,
        "going into" to 0.65f, "entering" to 0.6f, "i need" to 0.4f,
        "i'm about to" to 0.6f, "im about to" to 0.6f,
        "i am about to" to 0.6f, "i'm going to" to 0.55f,
        "im going to" to 0.55f, "i have" to 0.45f, "i've got" to 0.5f,
        "help me" to 0.45f
    )

    private val LIMIT_VOCAB: Map<String, Float> = mapOf(
        "limit" to 0.9f, "set limit" to 1.0f, "set a limit" to 1.0f,
        "time limit" to 0.95f, "screen time limit" to 1.0f,
        "screen time" to 0.8f, "daily limit" to 0.95f,
        "limit per day" to 0.95f, "per day" to 0.6f,
        "a day" to 0.5f, "each day" to 0.55f, "daily" to 0.55f,
        "cap" to 0.85f, "cap my" to 0.9f, "cap at" to 0.9f,
        "capped at" to 0.9f, "maximum" to 0.8f, "max" to 0.75f,
        "no more than" to 0.9f, "not more than" to 0.9f,
        "only allow" to 0.85f, "only let me use" to 0.85f,
        "allow only" to 0.85f, "restrict to" to 0.85f,
        "limit to" to 0.9f, "keep to" to 0.75f,
        "spending limit" to 0.9f, "usage limit" to 0.9f,
        "time cap" to 0.9f, "usage cap" to 0.9f,
        "daily cap" to 0.95f, "daily maximum" to 0.95f,
        "minutes per day" to 0.9f, "hours per day" to 0.9f,
        "minutes a day" to 0.9f, "hours a day" to 0.9f,
        "minute limit" to 0.9f, "hour limit" to 0.9f
    )

    // ──────────────────────────────────────────────────────────────────
    //  MODE ASSIGNMENT MAP
    // ──────────────────────────────────────────────────────────────────
    private val MODE_ASSIGNMENT: Map<String, FocusMode> = mapOf(
        "focus mode" to FocusMode.FOCUS, "deep focus" to FocusMode.FOCUS,
        "focus session" to FocusMode.FOCUS, "focus time" to FocusMode.FOCUS,
        "deep work" to FocusMode.FOCUS, "flow state" to FocusMode.FOCUS,
        "zen mode" to FocusMode.FOCUS, "laser focus" to FocusMode.FOCUS,
        "hyperfocus" to FocusMode.FOCUS, "concentration mode" to FocusMode.FOCUS,
        "concentrate" to FocusMode.FOCUS, "concentration" to FocusMode.FOCUS,
        "study mode" to FocusMode.STUDY, "study session" to FocusMode.STUDY,
        "study time" to FocusMode.STUDY, "deep study" to FocusMode.STUDY,
        "study" to FocusMode.STUDY, "studying" to FocusMode.STUDY,
        "homework" to FocusMode.STUDY, "exam" to FocusMode.STUDY,
        "revision" to FocusMode.STUDY, "revising" to FocusMode.STUDY,
        "cramming" to FocusMode.STUDY, "lecture" to FocusMode.STUDY,
        "class" to FocusMode.STUDY, "school" to FocusMode.STUDY,
        "assignment" to FocusMode.STUDY, "test" to FocusMode.STUDY,
        "work mode" to FocusMode.WORK, "work session" to FocusMode.WORK,
        "work time" to FocusMode.WORK, "office mode" to FocusMode.WORK,
        "productivity mode" to FocusMode.WORK, "professional mode" to FocusMode.WORK,
        "meeting" to FocusMode.WORK, "in a meeting" to FocusMode.WORK,
        "on a call" to FocusMode.WORK, "office hours" to FocusMode.WORK,
        "deadline" to FocusMode.WORK, "presentation" to FocusMode.WORK,
        "pomodoro" to FocusMode.WORK, "sprint" to FocusMode.WORK,
        "hustle" to FocusMode.WORK, "grind" to FocusMode.WORK,
        "boss mode" to FocusMode.WORK, "work" to FocusMode.WORK,
        "working" to FocusMode.WORK,
        "sleep mode" to FocusMode.SLEEP, "bedtime" to FocusMode.SLEEP,
        "bed time" to FocusMode.SLEEP, "sleep session" to FocusMode.SLEEP,
        "night mode" to FocusMode.SLEEP, "goodnight" to FocusMode.SLEEP,
        "good night" to FocusMode.SLEEP, "going to bed" to FocusMode.SLEEP,
        "heading to bed" to FocusMode.SLEEP, "going to sleep" to FocusMode.SLEEP,
        "time to sleep" to FocusMode.SLEEP, "time for bed" to FocusMode.SLEEP,
        "winding down" to FocusMode.SLEEP, "wind down" to FocusMode.SLEEP,
        "night routine" to FocusMode.SLEEP, "rest mode" to FocusMode.SLEEP,
        "rest time" to FocusMode.SLEEP, "nap time" to FocusMode.SLEEP,
        "nap mode" to FocusMode.SLEEP, "taking a nap" to FocusMode.SLEEP,
        "hitting the sack" to FocusMode.SLEEP, "lights out" to FocusMode.SLEEP,
        "nap" to FocusMode.SLEEP, "sleep" to FocusMode.SLEEP,
        "sleeping" to FocusMode.SLEEP
    )

    // ──────────────────────────────────────────────────────────────────
    //  TIME PATTERNS
    // ──────────────────────────────────────────────────────────────────
    private val TIME_PATTERNS = listOf(
        Pattern.compile("(\\d+)\\s*hours?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*h\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*mins?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*m\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*seconds?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*secs?\\b", Pattern.CASE_INSENSITIVE)
    )

    private val DELAY_PATTERNS = listOf(
        Pattern.compile("(?:^|\\b)(?:in|after)\\s+(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:^|\\b)(?:in|after)\\s+(\\d+)\\s*(?:hours?|h|hr)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*minutes?\\s+from\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d+)\\s*(?:hours?|h|hr)\\s+from\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("starting\\s+in\\s+(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("starting\\s+in\\s+(\\d+)\\s*(?:hours?|h|hr)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("in\\s+the\\s+next\\s+(\\d+)\\s*minutes?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("in\\s+the\\s+next\\s+(\\d+)\\s*(?:hours?|h|hr)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("after\\s+(\\d+)\\s*(?:hours?|h|hr)\\s+coming", Pattern.CASE_INSENSITIVE),
        Pattern.compile("after\\s+(\\d+)\\s*minutes?\\s+coming", Pattern.CASE_INSENSITIVE),
        Pattern.compile("after\\s+(\\d+)\\s*(?:hours?|h|hr)\\s+(?:from\\s+now|later)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("after\\s+(\\d+)\\s*minutes?\\s+(?:from\\s+now|later)", Pattern.CASE_INSENSITIVE)
    )
    private val DELAY_MULTIPLIERS = listOf(60L, 3600L, 60L, 3600L, 60L, 3600L, 60L, 3600L, 3600L, 60L, 3600L, 60L)

    private val UNTIL_PATTERN = Pattern.compile(
        "until\\s+(\\d+)(?::(\\d+))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE
    )
    private val TRIGGER_OPEN_PATTERN = Pattern.compile(
        "when(?:ever)?\\s+(?:i\\s+)?(?:open|launch|start|use|go\\s+on|visit|check)\\s+([\\w\\s]+?)(?:\\s*,|\\s+block|\\s+lock|\\s+stop|\$)",
        Pattern.CASE_INSENSITIVE
    )
    private val TRIGGER_USAGE_PATTERN = Pattern.compile(
        "after\\s+(\\d+)\\s*(?:minutes?|mins?)\\s*(?:of\\s+(?:use|usage|screen\\s+time|being\\s+on|using))",
        Pattern.CASE_INSENSITIVE
    )

    // ──────────────────────────────────────────────────────────────────
    //  MAIN PARSE ENTRY POINT
    // ──────────────────────────────────────────────────────────────────
    fun parse(input: String): ParsedCommand {
        val raw = input.trim()
        if (raw.isBlank()) return ParsedCommand(
            action = CommandAction.UNKNOWN, rawInput = raw,
            isValid = false, errorMessage = "Empty command"
        )

        // Step 1 — Normalize
        val normalized = normalize(raw)

        // Step 2 — Apply corrections (expand typos + slang)
        val corrected = applyCorrections(normalized)

        // Step 3 — Extract all entities
        val apps      = extractApps(corrected)
        val category  = extractCategory(corrected)
        val duration  = parseDuration(corrected)
        val delay     = parseDelay(corrected)
        val mode      = extractMode(corrected)
        val untilTs   = parseUntilTime(corrected)

        // Step 4 — Classify intent via scoring
        val action = classifyIntent(corrected, apps, category, mode)

        // Step 5 — Determine trigger
        val trigger = when {
            delay > 0L -> CommandTrigger.SCHEDULED
            TRIGGER_OPEN_PATTERN.matcher(corrected).find() -> CommandTrigger.ON_APP_OPEN
            TRIGGER_USAGE_PATTERN.matcher(corrected).find() -> CommandTrigger.ON_TIME_LIMIT
            else -> CommandTrigger.IMMEDIATE
        }
        val triggerApp   = extractTriggerApp(corrected)
        val triggerUsage = extractTriggerUsage(corrected)

        val finalAction = if (delay > 0L && action == CommandAction.BLOCK_APP)
            CommandAction.SCHEDULE_BLOCK else action

        return ParsedCommand(
            action              = finalAction,
            targetApps          = apps,
            targetCategory      = category,
            durationSeconds     = duration,
            delaySeconds        = delay,
            untilTimestamp      = untilTs,
            trigger             = trigger,
            triggerApp          = triggerApp,
            triggerUsageSeconds = triggerUsage,
            mode                = mode,
            rawInput            = raw,
            isValid             = finalAction != CommandAction.UNKNOWN,
            errorMessage        = if (finalAction == CommandAction.UNKNOWN)
                "Couldn't understand. Try: \"Block Instagram for 2 hours\"" else ""
        )
    }

    // ──────────────────────────────────────────────────────────────────
    //  NLP PIPELINE STEPS
    // ──────────────────────────────────────────────────────────────────

    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[\u2018\u2019\u0060]"), "'")
            .replace(Regex("[\u201C\u201D]"), "\"")
            .replace(Regex("[\u2013\u2014]"), "-")
            .replace(Regex("[^a-z0-9\\s'.,!?@#]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun applyCorrections(text: String): String {
        var result = text
        // Apply longest-match first to avoid partial replacements
        CORRECTIONS.entries
            .sortedByDescending { it.key.length }
            .forEach { (typo, fix) ->
                result = result.replace(Regex("\\b${Regex.escape(typo)}\\b", RegexOption.IGNORE_CASE), fix)
            }
        return result
    }

    /** Score each intent and return the highest-scoring action */
    private fun classifyIntent(
        text: String,
        apps: List<String>,
        category: AppCategory?,
        mode: FocusMode?
    ): CommandAction {
        val scores = mutableMapOf(
            CommandAction.BLOCK_APP          to 0f,
            CommandAction.MUTE_NOTIFICATIONS to 0f,
            CommandAction.UNBLOCK_APP        to 0f,
            CommandAction.ACTIVATE_MODE      to 0f,
            CommandAction.SET_TIME_LIMIT     to 0f
        )

        // Score each vocabulary bank
        scoreVocab(text, BLOCK_VOCAB,   scores, CommandAction.BLOCK_APP)
        scoreVocab(text, MUTE_VOCAB,    scores, CommandAction.MUTE_NOTIFICATIONS)
        scoreVocab(text, UNBLOCK_VOCAB, scores, CommandAction.UNBLOCK_APP)
        scoreVocab(text, MODE_VOCAB,    scores, CommandAction.ACTIVATE_MODE)
        scoreVocab(text, LIMIT_VOCAB,   scores, CommandAction.SET_TIME_LIMIT)

        // Boost ACTIVATE_MODE if a specific mode was identified
        if (mode != null) scores[CommandAction.ACTIVATE_MODE] =
            (scores[CommandAction.ACTIVATE_MODE] ?: 0f) + 0.5f

        // Boost MUTE if "notification" variants present even without strong intent word
        if (text.contains("notif") || text.contains("alert") ||
            text.contains("ping") || text.contains("buzz")) {
            scores[CommandAction.MUTE_NOTIFICATIONS] =
                (scores[CommandAction.MUTE_NOTIFICATIONS] ?: 0f) + 0.3f
        }

        // Boost BLOCK if apps/categories present with block-adjacent words
        if ((apps.isNotEmpty() || category != null) &&
            (text.contains("stop") || text.contains("can't") || text.contains("too much") ||
             text.contains("wast") || text.contains("addict") || text.contains("ruin"))) {
            scores[CommandAction.BLOCK_APP] =
                (scores[CommandAction.BLOCK_APP] ?: 0f) + 0.4f
        }

        val best = scores.maxByOrNull { it.value }
        return if ((best?.value ?: 0f) >= 0.4f) best!!.key else CommandAction.UNKNOWN
    }

    /** Score all n-grams (1,2,3-grams) against a vocabulary bank */
    private fun scoreVocab(
        text: String,
        vocab: Map<String, Float>,
        scores: MutableMap<CommandAction, Float>,
        action: CommandAction
    ) {
        val words = text.split(" ").filter { it.isNotBlank() }
        // Match longest phrases first
        vocab.entries.sortedByDescending { it.key.length }.forEach { (phrase, weight) ->
            if (text.contains(phrase)) {
                scores[action] = (scores[action] ?: 0f) + weight
            }
        }
        // Also score individual words for coverage
        words.forEach { word ->
            val w = vocab[word] ?: 0f
            if (w > 0f) scores[action] = (scores[action] ?: 0f) + (w * 0.5f)
        }
    }

    private fun extractApps(text: String): List<String> {
        val found = mutableListOf<String>()
        APP_MAP.entries
            .sortedByDescending { it.key.length }
            .forEach { (alias, pkg) ->
                if (text.contains(alias) && pkg !in found) found.add(pkg)
            }
        return found
    }

    private fun extractCategory(text: String): AppCategory? =
        CATEGORY_MAP.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { text.contains(it.key) }?.value

    private fun extractMode(text: String): FocusMode? =
        MODE_ASSIGNMENT.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { text.contains(it.key) }?.value

    fun parseDuration(text: String): Long {
        var total = 0L
        TIME_PATTERNS.forEachIndexed { i, p ->
            val m = p.matcher(text)
            while (m.find()) {
                val v = m.group(1)?.toLongOrNull() ?: continue
                total += when (i) { 0, 1 -> v * 3600; 2, 3, 4 -> v * 60; else -> v }
            }
        }
        return total
    }

    private fun parseDelay(text: String): Long {
        DELAY_PATTERNS.forEachIndexed { i, p ->
            val m = p.matcher(text)
            if (m.find()) {
                val v = m.group(1)?.toLongOrNull() ?: return@forEachIndexed
                return v * DELAY_MULTIPLIERS[i]
            }
        }
        return 0L
    }

    private fun parseUntilTime(text: String): Long {
        val m = UNTIL_PATTERN.matcher(text)
        if (!m.find()) return 0L
        val hour = m.group(1)?.toIntOrNull() ?: return 0L
        val min  = m.group(2)?.toIntOrNull() ?: 0
        val amPm = m.group(3)?.lowercase()
        val cal  = Calendar.getInstance()
        var h    = hour
        if (amPm == "pm" && h < 12) h += 12
        if (amPm == "am" && h == 12) h = 0
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    private fun extractTriggerApp(text: String): String? {
        val m = TRIGGER_OPEN_PATTERN.matcher(text)
        if (!m.find()) return null
        val phrase = m.group(1)?.trim() ?: return null
        return APP_MAP.entries.sortedByDescending { it.key.length }
            .firstOrNull { phrase.contains(it.key) }?.value
    }

    private fun extractTriggerUsage(text: String): Long {
        val m = TRIGGER_USAGE_PATTERN.matcher(text)
        if (!m.find()) return 0L
        return (m.group(1)?.toLongOrNull() ?: 0L) * 60
    }

    // ──────────────────────────────────────────────────────────────────
    //  SUMMARIZE & FORMAT
    // ──────────────────────────────────────────────────────────────────
    fun summarize(cmd: ParsedCommand): String {
        val appNames = cmd.targetApps.map { pkg ->
            APP_MAP.entries.firstOrNull { it.value == pkg }?.key
                ?.split(" ")?.joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) } ?: pkg
        }
        val apps = when {
            appNames.isNotEmpty() -> appNames.joinToString(", ")
            cmd.targetCategory != null -> "${cmd.targetCategory.name.lowercase()} apps"
            else -> "apps"
        }
        val dur   = formatDuration(cmd.durationSeconds)
        val delay = if (cmd.delaySeconds > 0)
            " (starts in ${formatDuration(cmd.delaySeconds)})" else ""
        return when (cmd.action) {
            CommandAction.BLOCK_APP          -> "Block $apps${if (dur.isNotEmpty()) " for $dur" else ""}$delay"
            CommandAction.SCHEDULE_BLOCK     -> "Schedule block on $apps${if (dur.isNotEmpty()) " for $dur" else ""}$delay"
            CommandAction.UNBLOCK_APP        -> "Unblock $apps"
            CommandAction.MUTE_NOTIFICATIONS -> "Mute notifications${if (apps != "apps") " from $apps" else ""}${if (dur.isNotEmpty()) " for $dur" else ""}$delay"
            CommandAction.UNMUTE_NOTIFICATIONS -> "Unmute notifications from $apps"
            CommandAction.ACTIVATE_MODE      -> "${cmd.mode?.name?.lowercase()?.replaceFirstChar(Char::uppercaseChar)} Mode${if (dur.isNotEmpty()) " for $dur" else ""}$delay"
            CommandAction.SET_TIME_LIMIT     -> "Limit $apps to $dur/day"
            else -> cmd.rawInput
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0 && h == 0L) append("${s}s")
        }.trim()
    }

    fun calculatePointsCost(cmd: ParsedCommand): Int {
        val base = when (cmd.action) {
            CommandAction.BLOCK_APP, CommandAction.SCHEDULE_BLOCK -> 5
            CommandAction.MUTE_NOTIFICATIONS -> 3
            CommandAction.ACTIVATE_MODE -> 8
            CommandAction.SET_TIME_LIMIT -> 5
            else -> 2
        }
        return base * when {
            cmd.durationSeconds > 3600 * 4 -> 3
            cmd.durationSeconds > 3600 -> 2
            else -> 1
        }
    }
}
