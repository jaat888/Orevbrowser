package com.privbrowse.app.privacy

/** Reader-mode page simplifier. It intentionally avoids network fetching or cloud parsing. */
object ReaderMode {
    val SCRIPT = """
        (function() {
          try {
            var selectors = ['article', 'main', '[role="main"]', '.article', '.post-content', '.entry-content'];
            var best = null, bestLen = 0;
            selectors.forEach(function(sel) {
              document.querySelectorAll(sel).forEach(function(el) {
                var len = (el.innerText || '').trim().length;
                if (len > bestLen) { best = el; bestLen = len; }
              });
            });
            var text = best ? best.innerText : document.body.innerText;
            var title = document.title || 'Reader mode';
            var safe = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
            document.documentElement.innerHTML = '<head><meta name="viewport" content="width=device-width,initial-scale=1"></head>'+
              '<body style="margin:0 auto;max-width:760px;padding:24px;font:18px/1.65 system-ui,sans-serif;background:#fff;color:#111">'+
              '<h1 style="font-size:32px;line-height:1.2">'+title.replace(/</g,'&lt;')+'</h1><pre style="white-space:pre-wrap;font:inherit">'+safe+'</pre></body>';
            return true;
          } catch(e) { return false; }
        })();
    """.trimIndent()
}
