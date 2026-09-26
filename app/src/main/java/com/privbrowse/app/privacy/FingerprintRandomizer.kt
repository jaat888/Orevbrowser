package com.privbrowse.app.privacy

/**
 * Per-tab "fresh identity" fingerprint noise.
 *
 * Real device identifiers (Android ID, IMEI, etc.) are never handed to a
 * webpage in the first place — WebView doesn't expose them to JavaScript,
 * so there's nothing to "regenerate" there. What sites actually use to
 * recognize a device is the *fingerprint*: canvas/audio output, WebGL
 * renderer strings, core count, memory, etc. This gives each tab its own
 * seed and skews that surface consistently for the life of the tab, so
 * a tracker that stitches fingerprints together sees a different "device"
 * per tab instead of one identity across all of them.
 *
 * The noise is deterministic per seed (not re-randomized on every call),
 * because fingerprinting libraries often sample twice and compare — a
 * value that changes on every read is itself a detectable signal.
 */
object FingerprintRandomizer {

    private val GPU_PROFILES = listOf(
        "ANGLE (Adreno (TM) 640)" to "Qualcomm",
        "ANGLE (Mali-G78)" to "ARM",
        "ANGLE (Adreno (TM) 730)" to "Qualcomm",
        "ANGLE (Xclipse 920)" to "Samsung",
        "ANGLE (Mali-G710)" to "ARM"
    )

    private const val HW_CONCURRENCY_CHOICES = "[4, 6, 8]"
    private const val DEVICE_MEMORY_CHOICES = "[2, 4, 8]"

    /**
     * Builds the injectable script for one tab. [seed] should be stable for
     * that tab's whole lifetime (its tab id works well) so the tab keeps a
     * consistent "identity" across page loads, but differs from every other
     * open tab.
     */
    fun script(seed: Long): String {
        val (renderer, vendor) = GPU_PROFILES[(seed.mod(GPU_PROFILES.size.toLong())).toInt()]
        return """
        (function() {
          // Deterministic PRNG seeded per-tab (mulberry32) so noise is
          // stable within a tab but differs across tabs.
          var seed = ${seed} >>> 0;
          function rand() {
            seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
            var t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
            t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
          }

          function define(obj, prop, value) {
            try {
              Object.defineProperty(obj, prop, { get: function() { return value; }, configurable: true });
            } catch (e) {}
          }

          // Hardware profile: nudge core count / memory to one of a few
          // common values instead of the device's real, more identifying one.
          var hwChoices = $HW_CONCURRENCY_CHOICES;
          var memChoices = $DEVICE_MEMORY_CHOICES;
          define(navigator, 'hardwareConcurrency', hwChoices[Math.floor(rand() * hwChoices.length)]);
          if ('deviceMemory' in navigator) {
            define(navigator, 'deviceMemory', memChoices[Math.floor(rand() * memChoices.length)]);
          }

          // Canvas fingerprint: add a faint, seed-stable per-pixel offset
          // before the page reads back pixel data.
          function noisyImageData(imageData) {
            var d = imageData.data;
            for (var i = 0; i < d.length; i += 4) {
              var n = Math.floor(rand() * 3) - 1;
              d[i] = Math.min(255, Math.max(0, d[i] + n));
              d[i + 1] = Math.min(255, Math.max(0, d[i + 1] + n));
              d[i + 2] = Math.min(255, Math.max(0, d[i + 2] + n));
            }
            return imageData;
          }
          var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
          CanvasRenderingContext2D.prototype.getImageData = function() {
            return noisyImageData(origGetImageData.apply(this, arguments));
          };
          var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
          HTMLCanvasElement.prototype.toDataURL = function() {
            try {
              var ctx = this.getContext('2d');
              if (ctx) noisyImageData(ctx.getImageData(0, 0, this.width, this.height));
            } catch (e) {}
            return origToDataURL.apply(this, arguments);
          };

          // WebGL vendor/renderer: report one plausible, shared GPU string
          // per tab instead of the device's exact, more unique one.
          function patchGl(proto) {
            var origGetParameter = proto.getParameter;
            proto.getParameter = function(param) {
              if (param === 37445) return '$vendor';
              if (param === 37446) return '$renderer';
              return origGetParameter.apply(this, arguments);
            };
          }
          try { if (window.WebGLRenderingContext) patchGl(WebGLRenderingContext.prototype); } catch (e) {}
          try { if (window.WebGL2RenderingContext) patchGl(WebGL2RenderingContext.prototype); } catch (e) {}

          // Audio fingerprint: tiny per-sample noise, same idea as canvas.
          try {
            var origGetChannelData = AudioBuffer.prototype.getChannelData;
            AudioBuffer.prototype.getChannelData = function() {
              var data = origGetChannelData.apply(this, arguments);
              for (var i = 0; i < data.length; i += 100) {
                data[i] = data[i] + (rand() - 0.5) * 0.0001;
              }
              return data;
            };
          } catch (e) {}
        })();
        """.trimIndent()
    }
}
