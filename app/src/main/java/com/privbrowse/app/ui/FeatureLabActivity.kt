package com.privbrowse.app.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.privbrowse.app.R
import com.privbrowse.app.privacy.BiometricLock
import com.privbrowse.app.privacy.SiteCustomizationStore
import com.privbrowse.app.ultimate.QrExport
import com.privbrowse.app.ultimate.UltimateFeatureStore

/** Real implementations for the high-value wishlist controls that fit safely on Android WebView. */
class FeatureLabActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE); build() }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(color(R.color.background_light)) }
        root.addView(toolbar())
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(5), dp(14), dp(30)) }
        list.addView(hero())
        section(list, "Tabs & navigation") {
            action(list, "Tab grid + search", "Open the searchable tab overview.") { startActivity(Intent(this, MainActivity::class.java).apply { action = MainActivity.ACTION_OPEN_TAB_MANAGER; addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }) }
            action(list, "Auto-close inactive tabs", "Configure and enable a timeout for normal inactive tabs.") { numberPref(MainActivity.KEY_AUTO_CLOSE_INACTIVE_HOURS, "Inactive-tab timeout (hours)", 0, 168) }
            action(list, "Set auto-close hours", "0 disables it; use a whole number such as 6 or 24.") { numberPref(MainActivity.KEY_AUTO_CLOSE_INACTIVE_HOURS, "Inactive-tab timeout (hours)", 0, 168) }
            toggle(list, "Self-destruct current tab", "Current tab is closed automatically after a countdown.", MainActivity.KEY_SELF_DESTRUCT_TABS, false)
            action(list, "Set self-destruct minutes", "Use this for a temporary tab; 0 disables the timer.") { numberPref(MainActivity.KEY_SELF_DESTRUCT_MINUTES, "Self-destruct minutes", 0, 1440) }
            action(list, "Tab groups / pinning", "Long-press a tab in the tab manager to pin it or assign a group.") { startActivity(Intent(this, MainActivity::class.java).apply { action = MainActivity.ACTION_OPEN_TAB_MANAGER; addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }) }
            toggle(list, "Edge-swipe navigation", "Swipe from the left/right screen edge for back/forward; swiping the address bar switches tabs.", MainActivity.KEY_EDGE_GESTURES, true)
        }
        section(list, "Address & search") {
            toggle(list, "Live address suggestions", "Suggest matching history, bookmarks and recent tabs while typing.", MainActivity.KEY_LIVE_SUGGESTIONS, true)
            toggle(list, "Clipboard link prompt", "Offer a copied HTTP/HTTPS link on browser start instead of silently reading it.", MainActivity.KEY_CLIPBOARD_PROMPT, true)
            toggle(list, "Bang / keyword commands", "Support !yt, !wiki, !maps and custom keyword:query commands.", MainActivity.KEY_BANG_COMMANDS, true)
            action(list, "Custom search keywords", "Create shortcuts such as 'yt:cats' or 'wiki:WebView'.") { keywordDialog() }
            action(list, "Compare two sites side-by-side", "Open two URLs in split WebViews.") { compareDialog() }
        }
        section(list, "Privacy & security") {
            toggle(list, "Ghost / RAM-first mode", "Use no-cache + no-history + clear-on-exit behavior as a practical WebView privacy mode.", MainActivity.KEY_GHOST_MODE, false)
            toggle(list, "Randomized web location", "Web pages receive a stable randomized coordinate per site; this does not change the device GPS location.", MainActivity.KEY_FAKE_GPS, false)
            toggle(list, "Block screenshots", "Android FLAG_SECURE for this app.", MainActivity.KEY_DISABLE_SCREEN_CAPTURE, false)
            action(list, "WebRTC / DNS leak tests", "Open two public diagnostics pages in new tabs.") { startTests() }
            action(list, "Biometric vault", "Protect sensitive tabs and app tools with the existing Android biometric gate.") { BiometricLock.authenticate(this, { prefs.edit().putBoolean(MainActivity.KEY_VAULT_LOCKED, true).apply(); toast("Vault lock enabled") }, { toast("Cancelled") }) }
            action(list, "Secure guest cleanup", "Clear browsing state, then open a fresh private tab.") { guestCleanup() }
            action(list, "DNS-over-HTTPS preference", "Select a preferred provider for diagnostics and future network modules; WebView DNS remains OS-controlled.") { dohDialog() }
        }
        section(list, "Content & productivity") {
            action(list, "Page image gallery", "Collect images from the current page into a clean gallery tab.") { openMain(MainActivity.ACTION_GALLERY) }
            action(list, "Subtitle candidate downloader", "Detect the first HTML5 <track> file and hand it to the download queue.") { openMain(MainActivity.ACTION_SUBTITLE_DOWNLOAD) }
            action(list, "Reverse image search", "Use the current page URL with an external image-search handoff.") { openMain(MainActivity.ACTION_REVERSE_IMAGE) }
            action(list, "Sticky notes / web clipper", "Open a real on-page sticky note or clip selected text/current screenshot.") { openMain(MainActivity.ACTION_STICKY_NOTE) }
            action(list, "Print-friendly page", "Build a simplified printable version of the current document.") { openMain(MainActivity.ACTION_PRINT_FRIENDLY) }
            action(list, "Calendar event helper", "Extract a page title and open a prefilled calendar event intent.") { openMain(MainActivity.ACTION_CALENDAR_EVENT) }
        }
        section(list, "Accessibility & customization") {
            toggle(list, "Dyslexia-friendly font", "Use a heavier, slightly spaced sans-serif presentation through page CSS.", MainActivity.KEY_DYSLEXIA_FONT, false)
            toggle(list, "High contrast page", "Increase foreground/background contrast with a page-level style layer.", MainActivity.KEY_HIGH_CONTRAST, false)
            toggle(list, "Large touch targets", "Keep browser action controls in the larger touch-size range.", MainActivity.KEY_LARGE_TOUCH, true)
            action(list, "Per-site CSS", "Save user CSS for the current domain.") { siteCodeDialog(css = true) }
            action(list, "Per-site JS", "Save a user script for the current domain.") { siteCodeDialog(css = false) }
        }
        section(list, "Developer & power user") {
            action(list, "Command palette", "Open a searchable list of browser actions.") { openCommandPalette() }
            action(list, "Network request log", "Use the existing transparency log as a per-tab network inspector.") { openMain(MainActivity.ACTION_SHOW_DIAGNOSTICS) }
            action(list, "User-agent switcher", "Toggle desktop/mobile mode for the active tab.") { openMain(MainActivity.ACTION_TOGGLE_DESKTOP) }
            action(list, "Full settings export/import", "Export or import every browser preference as one compressed backup file.") { startActivity(Intent(this, com.privbrowse.app.ultimate.BackupActivity::class.java)) }
        }
        section(list, "Ultimate tabs & device features") {
            toggleUltimate(list,"Vertical tab rail","Show a tablet-friendly vertical tab manager.",UltimateFeatureStore.KEY_VERTICAL_TABS,true)
            toggleUltimate(list,"Random tab close order","Bulk-close tabs in a randomized order.",UltimateFeatureStore.KEY_RANDOM_CLOSE,false)
            toggleUltimate(list,"Floating browser bubble","Show a chat-head style PrivBrowse launcher over other apps.",UltimateFeatureStore.KEY_FLOATING_BUBBLE,false)
            action(list,"Open second browser window","Launch another resizable PrivBrowse task for Android split-screen/multi-window."){openMain(MainActivity.ACTION_MULTI_WINDOW)}
            action(list,"Vault tab","Biometric-unlock and open a hidden tab."){openMain(MainActivity.ACTION_VAULT_TAB)}
            action(list,"Unlock vault + show tabs","Authenticate to reveal hidden vault tabs in the tab manager."){openMain(MainActivity.ACTION_UNLOCK_VAULT)}
            action(list,"QR tab transfer","Show the current normal-tab set as an offline QR package."){openMain(MainActivity.ACTION_QR_TRANSFER)}
        }
        section(list, "Search, media & page power") {
            action(list,"Compare search engines","Compare the same query in two search engines side by side."){openMain(MainActivity.ACTION_COMPARE_SEARCH)}
            toggleUltimate(list,"On-page AI chat bubble","Show a small AI button over the active page; it opens the existing page-context AI Copilot.",UltimateFeatureStore.KEY_AI_BUBBLE,false)
            action(list,"Video speed / audio-only / ad-skip","Set 0.5x–3x speed, audio-only rendering and common skip buttons."){openMain(MainActivity.ACTION_MEDIA_TOOLS)}
            action(list,"Picture-in-picture","Enter Android PiP while a supported video page is open."){openMain(MainActivity.ACTION_ENTER_PIP)}
            toggleUltimate(list,"AI junk/ad stripping","Remove common cookie/newsletter/advertisement overlays after page load.",UltimateFeatureStore.KEY_READER_STRIP,false)
            toggleUltimate(list,"Background media","Allow media playback without requiring a tap where WebView permits it.",UltimateFeatureStore.KEY_BACKGROUND_MEDIA,false)
            action(list,"Full-page screenshot","Capture the complete scrollable page and share it."){prefs.edit().putBoolean(MainActivity.KEY_FULL_PAGE_SCREENSHOT,true).apply();openMain(MainActivity.ACTION_CAPTURE_SCREENSHOT)}
            action(list,"QR current URL","Open QR transfer screen with current tab data."){openMain(MainActivity.ACTION_QR_TRANSFER)}
            action(list,"PDF viewer + annotate","Open a local PDF with pen/highlight tools and export an annotated PDF."){openMain(MainActivity.ACTION_PDF_ANNOTATE)}
        }
        section(list, "Monitoring, alerts & privacy dashboard") {
            action(list,"Price-drop / page-change / keyword alerts","Add a local background watcher; checks approximately every six hours."){startActivity(Intent(this, com.privbrowse.app.ultimate.WatchActivity::class.java))}
            action(list,"Weekly trust / privacy report","Review seven-day tracker/fingerprint totals, streaks and badges."){openMain(MainActivity.ACTION_PRIVACY_DASHBOARD)}
            toggleUltimate(list,"Unvalidated Wi-Fi warning","Warn when the current Wi-Fi network is not validated by Android.",UltimateFeatureStore.KEY_WIFI_WARNING,true)
            toggleUltimate(list,"Low-power mode","Throttle page animation when the battery is critically low.",UltimateFeatureStore.KEY_LOW_POWER,true)
            toggle(list,"Auto-clear clipboard","Clear copied sensitive values after the configured timeout.",MainActivity.KEY_AUTO_CLEAR_CLIPBOARD,false)
            action(list,"Clipboard timeout","Choose 5/15/30/60 seconds for automatic clipboard clearing."){val e=EditText(this).apply{inputType=InputType.TYPE_CLASS_NUMBER;setText(prefs.getLong(MainActivity.KEY_AUTO_CLEAR_CLIPBOARD_SECONDS,30).toString())};AlertDialog.Builder(this).setTitle("Clipboard timeout").setView(e).setPositiveButton("Save"){_,_->toast("Saved");prefs.edit().putLong(MainActivity.KEY_AUTO_CLEAR_CLIPBOARD_SECONDS,e.text.toString().toLongOrNull()?.coerceIn(5,300)?:30).apply()}.setNegativeButton("Cancel",null).show()}
        }
        section(list, "Dashboard, media & profiles") {
            action(list,"Home dashboard","In-app widgets for weather, RSS/Atom, todos and recent browsing."){startActivity(Intent(this,com.privbrowse.app.ultimate.DashboardActivity::class.java))}
            action(list,"RSS / Atom reader","Read local feeds without a third-party reader app."){startActivity(Intent(this,com.privbrowse.app.ultimate.RssActivity::class.java))}
            action(list,"Background audio","Keep a direct audio/media URL playing in a foreground media service."){openMain(MainActivity.ACTION_BACKGROUND_AUDIO)}
            action(list,"Profiles","Switch personal/work/guest WebView data directories; app restarts to apply the profile."){openMain(MainActivity.ACTION_PROFILES)}
            action(list,"Change app icon","Switch launcher icon using Android activity aliases."){val names=arrayOf("Default","Tools","Home");AlertDialog.Builder(this).setTitle("Launcher icon").setItems(names){_,w->com.privbrowse.app.ultimate.IconManager.set(this,names[w]);toast("Icon changed")}.show()}
            action(list,"Local tab package share","Export the real open-tab package through Android's offline share sheet; Bluetooth/Quick Share/Wi‑Fi peers can receive it."){openMain(MainActivity.ACTION_SHARE_TABS_LOCAL)}
        }
        section(list, "Downloads, backup & offline") {
            action(list,"Download queue","Batch direct downloads with resume support and optional speed limiting."){openMain(MainActivity.ACTION_DOWNLOAD_QUEUE)}
            action(list,"Set download speed limit","0 = unlimited; enter bytes/second for a local queue limiter."){numberUltimate("download_speed_bps","Speed limit (bytes/sec)",0,100000000)}
            action(list,"Offline archive mode","Save MHTML archives and reopen them from local storage when offline."){openMain(MainActivity.ACTION_SAVE_ARCHIVE)}
            action(list,"Export settings / import settings","Export or restore the full compressed settings snapshot."){startActivity(Intent(this,com.privbrowse.app.ultimate.BackupActivity::class.java))}
            action(list,"Reading streak & badges","Open the local privacy dashboard to see streak/achievement progress."){openMain(MainActivity.ACTION_PRIVACY_DASHBOARD)}
        }
        section(list, "Customization, scripting & power-user") {
            action(list,"Selection translate","Select/copy text and use Android's share/translate flow; the app also supports per-page translate."){openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS)}
            action(list,"Per-site CSS","Store CSS for the current/entered host."){siteCodeDialog(true)}
            action(list,"Per-site JS / userscript","Store JavaScript for a host; injected after page load."){siteCodeDialog(false)}
            action(list,"Inspect page / source","Use page source and DOM extraction as a safe mini developer tool."){openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS)}
            action(list,"Per-tab network inspector","Open the existing transparency log filtered to the active origin."){openMain(MainActivity.ACTION_SHOW_DIAGNOSTICS)}
            action(list,"Proxy / VPN controls","Open the existing VPN/Xray tools; kill-switch enforcement uses Android VPN settings when available."){startActivity(Intent(this,VpnActivity::class.java))}
            action(list,"Command palette","Search common actions with a keyboard-style palette."){openCommandPalette()}
        }
        section(list, "Accessibility & system privacy") {
            toggle(list,"Dyslexia-friendly font","Use spaced sans-serif page typography.",MainActivity.KEY_DYSLEXIA_FONT,false)
            toggle(list,"High contrast","Apply high-contrast page styling.",MainActivity.KEY_HIGH_CONTRAST,false)
            toggle(list,"Large touch targets","Keep action controls in the larger touch size range.",MainActivity.KEY_LARGE_TOUCH,true)
            toggleUltimate(list,"Ghost session","Do not persist a normal browsing session; clear browser state on exit.",UltimateFeatureStore.KEY_GHOST,false)
            action(list,"DNS / WebRTC leak checks","Run browserleaks WebRTC and DNS tests."){startTests()}
            action(list,"VPN always-on / lockdown settings","Open Android VPN settings where supported so the platform can enforce always-on/lockdown policies."){startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS))}
            action(list,"Floating bubble","Enable the overlay launcher after granting overlay permission."){openMain(MainActivity.ACTION_FLOATING_BUBBLE)}
        }
        section(list, "Remaining wishlist controls") {
            action(list,"QR current URL","Create an offline QR code for only the active page."){openMain(MainActivity.ACTION_QR_CURRENT_URL)}
            action(list,"Voice command browsing","Use Android speech recognition for browser navigation and search."){openMain(MainActivity.ACTION_VOICE_COMMAND)}
            action(list,"Email alias / masking","Generate a unique plus-address locally; no alias server is used."){openMain(MainActivity.ACTION_EMAIL_ALIAS)}
            action(list,"Calculator / unit converter","Local calculator plus km/mi/m/ft, kg/lb and C/F conversion."){openMain(MainActivity.ACTION_CALC_CONVERTER)}
            action(list,"Zoom / Meet one-tap join","Detect meeting links on the current page and hand off to the installed app."){openMain(MainActivity.ACTION_JOIN_VIDEO_CALL)}
            action(list,"AI image alt text","Send current-page image metadata into the existing AI Copilot with an accessibility prompt."){openMain(MainActivity.ACTION_AI_IMAGES)}
            action(list,"Theme library / JSON import","Apply bundled themes or import a local JSON theme for PrivBrowse surfaces."){startActivity(Intent(this, com.privbrowse.app.ultimate.ThemeActivity::class.java))}
            action(list,"Custom gesture editor","Assign left/right/up/down swipes to browser actions."){startActivity(Intent(this, com.privbrowse.app.ultimate.GestureEditorActivity::class.java))}
            action(list,"Crash log export","Copy the latest crash-safe stack trace captured before process death."){openMain(MainActivity.ACTION_CRASH_LOG)}
            toggleUltimate(list,"Offline/cache-only mode","Prevent network fetches and use cached WebView content where available.",UltimateFeatureStore.KEY_OFFLINE_MODE,false)
            toggleUltimate(list,"VPN kill switch","When enabled, PrivBrowse blocks WebView requests while no VPN transport is active.",UltimateFeatureStore.KEY_VPN_KILL_SWITCH,false)
            toggleUltimate(list,"Auto-revoke media session","After backgrounding, deny future web camera/microphone requests until re-enabled.",UltimateFeatureStore.KEY_AUTO_REVOKE_MEDIA,false)
            toggleUltimate(list,"Site-isolation cleanup","Clear site cookies/storage when moving between top-level origins (privacy-first, login-hostile).",UltimateFeatureStore.KEY_SITE_ISOLATION,false)
            toggleUltimate(list,"Network usage counters","Track per-page resource-request counts for a lightweight local usage report.",UltimateFeatureStore.KEY_NETWORK_USAGE,true)
            toggleUltimate(list,"One-handed compact mode","Shift browser controls toward the lower-right for one-handed use.",UltimateFeatureStore.KEY_ONE_HANDED,false)
            toggleUltimate(list,"Scheduled midnight history clear","Clear local browsing history automatically around midnight.",UltimateFeatureStore.KEY_SCHEDULED_CLEAR_HISTORY,false)
            action(list,"Scheduled local backup now","Write a compressed settings backup to PrivBrowse app storage."){runCatching{com.privbrowse.app.ultimate.BackupManager.shareLatest(this)}.onFailure{toast("Backup failed: ${it.message}")}}
            action(list,"Bluetooth / Wi-Fi local share","Use Android's local-share/nearby transports for the encrypted tab-package payload; no PrivBrowse server is involved."){openMain(MainActivity.ACTION_SHARE_TABS_LOCAL)}
            action(list,"Background media controls","Start page audio in the foreground media service so the playback session survives tab closure."){openMain(MainActivity.ACTION_BACKGROUND_AUDIO)}
            action(list,"Profiles / Guest","Separate personal/work/guest WebView data directories and cookies."){openMain(MainActivity.ACTION_PROFILES)}
            action(list,"App icon changer","Switch among the bundled launcher aliases without reinstalling."){iconDialog()}
        }
        section(list, "Wishlist coverage") {
            action(list, "View implementation status", "Read the included wishlist status file: native features, WebView limitations and Android-platform limits.") { toast("See WISHLIST_IMPLEMENTATION.md in the source zip") }
        }
        scroll.addView(list); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
    }

    private fun toolbar() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(6),dp(4),dp(4)); addView(TextView(this@FeatureLabActivity).apply { text="‹"; textSize=38f; gravity=Gravity.CENTER; setTextColor(color(R.color.text_light)); setOnClickListener{finish()} }, LinearLayout.LayoutParams(dp(44),dp(50))); addView(TextView(this@FeatureLabActivity).apply { text="Ultimate Feature Lab"; textSize=22f; setTextColor(color(R.color.text_light)); setTypeface(typeface,android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0,-2,1f)) }
    private fun hero() = MaterialCardView(this).apply { radius=dp(18).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light));addView(LinearLayout(this@FeatureLabActivity).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(14),dp(15),dp(14));addView(TextView(this@FeatureLabActivity).apply{text="High-value wishlist controls are wired here";textSize=17f;setTextColor(color(R.color.text_light));setTypeface(typeface,android.graphics.Typeface.BOLD)});addView(TextView(this@FeatureLabActivity).apply{text="The app keeps privacy-sensitive features explicit and labels Android/WebView limitations instead of faking support.";textSize=12f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(4),0,0)}) }) }
    private fun section(parent: LinearLayout, title: String, body: () -> Unit) { parent.addView(TextView(this).apply{text=title.uppercase();textSize=11.5f;setTextColor(color(R.color.accent_dark));setTypeface(typeface,android.graphics.Typeface.BOLD);setPadding(dp(2),dp(18),0,dp(6))}); body() }
    private fun card(title:String,sub:String,onClick:()->Unit):View=MaterialCardView(this).apply{radius=dp(14).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light));setOnClickListener{onClick()};addView(LinearLayout(this@FeatureLabActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(11),dp(12),dp(11));addView(TextView(this@FeatureLabActivity).apply{text=title;textSize=14.2f;setTextColor(color(R.color.text_light));setTypeface(typeface,android.graphics.Typeface.BOLD)});addView(TextView(this@FeatureLabActivity).apply{text=sub;textSize=11.3f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(2),0,0);maxLines=2})})}
    private fun action(parent:LinearLayout,title:String,sub:String,onClick:()->Unit){parent.addView(card(title,sub,onClick),lp().apply{bottomMargin=dp(6)})}
    private fun toggle(parent:LinearLayout,title:String,sub:String,key:String,default:Boolean){val c=MaterialCardView(this).apply{radius=dp(14).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light))};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(8),dp(10))};val copy=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};copy.addView(TextView(this@FeatureLabActivity).apply{text=title;textSize=14.2f;setTextColor(color(R.color.text_light));setTypeface(typeface,android.graphics.Typeface.BOLD)});copy.addView(TextView(this@FeatureLabActivity).apply{text=sub;textSize=11.3f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(2),0,0);maxLines=2});val sw=MaterialSwitch(this@FeatureLabActivity).apply{isChecked=prefs.getBoolean(key,default);setOnCheckedChangeListener{_,v->prefs.edit().putBoolean(key,v).apply();if(key==MainActivity.KEY_GHOST_MODE&&v) prefs.edit().putBoolean(MainActivity.KEY_NO_CACHE,true).putBoolean(MainActivity.KEY_SAVE_HISTORY,false).putBoolean(MainActivity.KEY_CLEAR_ON_EXIT,true).apply()}};row.addView(copy,LinearLayout.LayoutParams(0,-2,1f));row.addView(sw);c.addView(row);parent.addView(c,lp().apply{bottomMargin=dp(6)})}

    private fun toggleUltimate(parent:LinearLayout,title:String,sub:String,key:String,default:Boolean){val c=MaterialCardView(this).apply{radius=dp(14).toFloat();cardElevation=0f;strokeWidth=dp(1);strokeColor=color(R.color.divider);setCardBackgroundColor(color(R.color.surface_light))};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(8),dp(10))};val copy=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};copy.addView(TextView(this@FeatureLabActivity).apply{text=title;textSize=14.2f;setTextColor(color(R.color.text_light));setTypeface(typeface,android.graphics.Typeface.BOLD)});copy.addView(TextView(this@FeatureLabActivity).apply{text=sub;textSize=11.3f;setTextColor(color(R.color.text_muted_light));setPadding(0,dp(2),0,0);maxLines=2});val sw=MaterialSwitch(this@FeatureLabActivity).apply{isChecked=UltimateFeatureStore.prefs(this@FeatureLabActivity).getBoolean(key,default);setOnCheckedChangeListener{_,v->UltimateFeatureStore.prefs(this@FeatureLabActivity).edit().putBoolean(key,v).apply();if(key==UltimateFeatureStore.KEY_SCHEDULED_CLEAR_HISTORY)com.privbrowse.app.ultimate.CleanupReceiver.schedule(this@FeatureLabActivity)}};row.addView(copy,LinearLayout.LayoutParams(0,-2,1f));row.addView(sw);c.addView(row);parent.addView(c,lp().apply{bottomMargin=dp(6)})}
    private fun numberUltimate(key:String,title:String,min:Long,max:Long){val e=EditText(this).apply{inputType=InputType.TYPE_CLASS_NUMBER;setText(UltimateFeatureStore.prefs(this@FeatureLabActivity).getLong(key,0).toString());selectAll()};AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Save"){_,_->UltimateFeatureStore.prefs(this).edit().putLong(key,e.text.toString().toLongOrNull()?.coerceIn(min,max)?:0).apply();toast("Saved")}.setNegativeButton("Cancel",null).show()}
    private fun numberPref(key:String,title:String,min:Int,max:Int){val e=EditText(this).apply{inputType=InputType.TYPE_CLASS_NUMBER;setText(prefs.getInt(key,0).toString());selectAll()};AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val n=e.text.toString().toIntOrNull()?.coerceIn(min,max)?:0;prefs.edit().putInt(key,n).apply();toast("Saved")}.show()}
    private fun keywordDialog(){val e=EditText(this).apply{hint="yt=https://www.youtube.com/results?search_query=%s\nwiki=https://en.wikipedia.org/wiki/Special:Search?search=%s";minLines=4;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;setText(prefs.getString(MainActivity.KEY_SEARCH_KEYWORDS,"") )};AlertDialog.Builder(this).setTitle("Custom search keywords").setView(e).setPositiveButton("Save"){_,_->prefs.edit().putString(MainActivity.KEY_SEARCH_KEYWORDS,e.text.toString()).apply();toast("Saved")}.setNegativeButton("Cancel",null).show()}
    private fun compareDialog(){val a=EditText(this).apply{hint="First URL";setSingleLine(true)};val b=EditText(this).apply{hint="Second URL";setSingleLine(true)};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(a);addView(b,lp().apply{topMargin=dp(8)})};AlertDialog.Builder(this).setTitle("Compare sites").setView(box).setPositiveButton("Open"){_,_->startActivity(Intent(this,CompareActivity::class.java).putExtra("left",a.text.toString()).putExtra("right",b.text.toString()))}.setNegativeButton("Cancel",null).show()}
    private fun siteCodeDialog(css:Boolean){val e=EditText(this).apply{hint=if(css)"body { filter: contrast(1.08); }" else "document.title = 'PrivBrowse';";minLines=7;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE};val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;val h=EditText(this@FeatureLabActivity).apply{hint="example.com";setSingleLine(true)};addView(h);addView(e,lp().apply{topMargin=dp(8)})};AlertDialog.Builder(this).setTitle(if(css)"Per-site CSS" else "Per-site JS").setView(box).setPositiveButton("Save"){_,_->val host=h.text.toString().trim().lowercase();if(host.isNotBlank()){if(css)SiteCustomizationStore.setCss(this,host,e.text.toString()) else SiteCustomizationStore.setJs(this,host,e.text.toString());toast("Saved for $host")}}.setNegativeButton("Cancel",null).show()}
    private fun iconDialog(){
        val names=arrayOf("Default","Tools","Home")
        val pm=getPackageManager()
        AlertDialog.Builder(this).setTitle("App icon").setItems(names){_,which->com.privbrowse.app.ultimate.IconManager.set(this,names[which]);toast("Icon changed to ${names[which]}")}.show()
    }

    private fun startTests(){startActivity(Intent(this,MainActivity::class.java).apply{action=MainActivity.ACTION_OPEN_LEAK_TESTS;addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)})}
    private fun guestCleanup(){startActivity(Intent(this,MainActivity::class.java).apply{action=MainActivity.ACTION_GUEST_CLEANUP;addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)})}
    private fun dohDialog(){val providers=arrayOf("System / automatic","Cloudflare 1.1.1.1","Google 8.8.8.8","Quad9 9.9.9.9");val current=providers.indexOf(prefs.getString(MainActivity.KEY_DOH_PROVIDER,providers[0])).coerceAtLeast(0);AlertDialog.Builder(this).setTitle("DNS preference").setSingleChoiceItems(providers,current){d,w->prefs.edit().putString(MainActivity.KEY_DOH_PROVIDER,providers[w]).apply();d.dismiss();toast("Preference saved; Android WebView DNS is still system-controlled")}.show()}
    private fun openCommandPalette(){val actions=arrayOf("Tab manager","Page tools","Feature Center","Notes","Compare","Private tab","Downloads","Privacy score","Diagnostics");AlertDialog.Builder(this).setTitle("Command palette").setItems(actions){_,w->when(w){0->openMain(MainActivity.ACTION_OPEN_TAB_MANAGER);1->openMain(MainActivity.ACTION_OPEN_PAGE_TOOLS);2->startActivity(Intent(this,FeatureCenterActivity::class.java));3->openMain(MainActivity.ACTION_NOTES);4->compareDialog();5->openMain(MainActivity.ACTION_OPEN_PRIVATE_TAB);6->openMain(MainActivity.ACTION_OPEN_DOWNLOADS);7->openMain(MainActivity.ACTION_SHOW_PRIVACY_SCORE);8->openMain(MainActivity.ACTION_SHOW_DIAGNOSTICS)}}.show()}
    private fun openMain(action:String){startActivity(Intent(this,MainActivity::class.java).apply{this.action=action;addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)})}
    private fun toast(s:String)=android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_SHORT).show()
}
