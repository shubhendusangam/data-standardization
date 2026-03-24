/**
 * PreviewPanel — reusable before/after diff component.
 * Zero dependencies · vanilla JS · CSS injected once.
 *
 * Usage:
 *   PreviewPanel.render(containerEl, {
 *     original:       [ {col:val, …}, … ],
 *     standardized:   [ {col:val, …}, … ],
 *     changedFields:  { "fieldName": "Rule Name", … }  // optional
 *     elapsedMs:      42                                 // optional
 *   });
 *
 * Data source: POST /api/standardization/preview?maxRecords=N
 */
var PreviewPanel = (function () {
  'use strict';

  /* ── Inject styles once ─────────────────────────────────────── */
  var _stylesInjected = false;
  function injectStyles() {
    if (_stylesInjected) return;
    _stylesInjected = true;
    var css = [
      '.pp-root{font-family:var(--font-sans,"Inter",-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif);font-size:.82rem;color:var(--color-text,#e1e4ed)}',
      '.pp-summary{display:flex;flex-wrap:wrap;gap:14px;padding:12px 16px;border-radius:6px;background:var(--color-surface-alt,#1e222d);border:1px solid var(--color-border,#2a2f3e);margin-bottom:14px;font-size:.76rem;color:var(--color-text-dim,#8b90a0)}',
      '.pp-summary .pp-stat{font-weight:700;color:var(--color-text,#e1e4ed)}',
      '.pp-diff-wrap{display:grid;grid-template-columns:1fr 1fr;gap:2px;border:1px solid var(--color-border,#2a2f3e);border-radius:6px;overflow:hidden;max-height:420px}',
      '.pp-diff-col{overflow:auto}',
      '.pp-diff-col.pp-synced{overflow-x:hidden}',
      '.pp-diff-label{position:sticky;top:0;z-index:2;padding:8px 12px;font-size:.68rem;text-transform:uppercase;letter-spacing:.06em;font-weight:700}',
      '.pp-diff-label.original{background:rgba(248,113,113,.12);color:#f87171}',
      '.pp-diff-label.standardized{background:rgba(52,211,153,.12);color:#34d399}',
      '.pp-diff-table{width:100%;border-collapse:collapse}',
      '.pp-diff-table th{position:sticky;top:30px;z-index:1;background:var(--color-surface,#181b23);font-size:.68rem;text-transform:uppercase;letter-spacing:.04em;color:var(--color-text-dim,#8b90a0);font-weight:600;padding:6px 10px;text-align:left;border-bottom:1px solid var(--color-border,#2a2f3e)}',
      '.pp-diff-table td{padding:5px 10px;border-bottom:1px solid var(--color-border,#2a2f3e);white-space:nowrap;font-family:var(--font-mono,"Consolas",monospace);font-size:.76rem}',
      '.pp-diff-table tr:hover td{background:rgba(108,140,255,.06)}',
      '.pp-changed{background:rgba(251,191,36,.12) !important;position:relative}',
      '.pp-changed::after{content:attr(data-rule);position:absolute;bottom:100%;left:50%;transform:translateX(-50%);background:#1e222d;color:#fbbf24;padding:3px 8px;border-radius:4px;font-size:.64rem;white-space:nowrap;pointer-events:none;opacity:0;transition:opacity .15s}',
      '.pp-changed:hover::after{opacity:1}',
      '.pp-export-bar{display:flex;gap:8px;margin-top:12px}',
      '.pp-btn{display:inline-flex;align-items:center;gap:5px;padding:7px 14px;border:1px solid var(--color-border,#2a2f3e);border-radius:6px;background:transparent;color:var(--color-text-dim,#8b90a0);font-size:.74rem;font-weight:600;cursor:pointer;font-family:inherit;transition:all .2s}',
      '.pp-btn:hover{color:var(--color-accent,#6c8cff);border-color:var(--color-accent,#6c8cff)}',
      '.pp-empty{text-align:center;padding:32px;color:var(--color-text-dim,#8b90a0)}'
    ].join('\n');
    var style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);
  }

  /* ── Helpers ────────────────────────────────────────────────── */
  function esc(s) {
    var d = document.createElement('div');
    d.textContent = s == null ? '' : String(s);
    return d.innerHTML.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function collectCols(original, standardized) {
    var set = {};
    var cols = [];
    [original, standardized].forEach(function (arr) {
      (arr || []).forEach(function (r) {
        Object.keys(r).forEach(function (k) {
          if (!set[k]) { set[k] = true; cols.push(k); }
        });
      });
    });
    return cols;
  }

  function computeChanges(original, standardized, cols) {
    var changed = 0;
    var changedFieldSet = {};
    var n = Math.min(original.length, standardized.length);
    for (var i = 0; i < n; i++) {
      var rowChanged = false;
      cols.forEach(function (c) {
        var a = original[i][c];
        var b = standardized[i][c];
        if (String(a == null ? '' : a) !== String(b == null ? '' : b)) {
          rowChanged = true;
          changedFieldSet[c] = true;
        }
      });
      if (rowChanged) changed++;
    }
    return { recordsChanged: changed, fieldsChanged: Object.keys(changedFieldSet).length };
  }

  function downloadBlob(content, filename, mime) {
    var blob = new Blob([content], { type: mime });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { document.body.removeChild(a); URL.revokeObjectURL(url); }, 100);
  }

  function toCsv(records, cols) {
    var lines = [cols.map(function (c) { return '"' + c.replace(/"/g, '""') + '"'; }).join(',')];
    records.forEach(function (r) {
      lines.push(cols.map(function (c) {
        var v = r[c] == null ? '' : String(r[c]);
        return '"' + v.replace(/"/g, '""') + '"';
      }).join(','));
    });
    return lines.join('\n');
  }

  /* ── Public API ─────────────────────────────────────────────── */
  function render(containerEl, opts) {
    injectStyles();
    if (!containerEl) return;

    var original     = opts.original || [];
    var standardized = opts.standardized || [];
    var changedFields = opts.changedFields || {};
    var elapsedMs    = opts.elapsedMs;

    if (!original.length && !standardized.length) {
      containerEl.innerHTML = '<div class="pp-empty">No data to preview.</div>';
      return;
    }

    var cols = collectCols(original, standardized);
    var stats = computeChanges(original, standardized, cols);

    /* ── Summary strip ──────────────────────────────────────── */
    var summaryHtml =
      '<div class="pp-summary">' +
        '<span><span class="pp-stat">' + stats.recordsChanged + '</span> of ' + original.length + ' records changed</span>' +
        '<span><span class="pp-stat">' + stats.fieldsChanged + '</span> fields affected</span>' +
        (elapsedMs != null ? '<span><span class="pp-stat">' + elapsedMs + '</span>ms</span>' : '') +
      '</div>';

    /* ── Build diff tables ──────────────────────────────────── */
    function buildTableHtml(records, labelClass, labelText, isStandardized) {
      var html = '<div class="pp-diff-col" data-side="' + labelClass + '">';
      html += '<div class="pp-diff-label ' + labelClass + '">' + labelText + '</div>';
      html += '<table class="pp-diff-table"><thead><tr>';
      cols.forEach(function (c) { html += '<th>' + esc(c) + '</th>'; });
      html += '</tr></thead><tbody>';
      var maxRows = Math.min(records.length, 200);
      for (var i = 0; i < maxRows; i++) {
        html += '<tr>';
        cols.forEach(function (c) {
          var val = records[i][c];
          var cls = '';
          var ruleAttr = '';
          if (isStandardized && original[i]) {
            var ov = original[i][c];
            if (String(ov == null ? '' : ov) !== String(val == null ? '' : val)) {
              cls = ' class="pp-changed"';
              var ruleName = changedFields[c] || 'modified';
              ruleAttr = ' data-rule="' + esc(ruleName) + '"';
            }
          }
          html += '<td' + cls + ruleAttr + '>' + esc(val) + '</td>';
        });
        html += '</tr>';
      }
      html += '</tbody></table></div>';
      return html;
    }

    var diffHtml =
      '<div class="pp-diff-wrap">' +
        buildTableHtml(original, 'original', 'Original', false) +
        buildTableHtml(standardized, 'standardized', 'Standardized', true) +
      '</div>';

    /* ── Export bar ──────────────────────────────────────────── */
    var exportHtml =
      '<div class="pp-export-bar">' +
        '<button class="pp-btn" data-pp-export="csv">⬇ Export CSV</button>' +
        '<button class="pp-btn" data-pp-export="json">⬇ Export JSON</button>' +
      '</div>';

    containerEl.innerHTML = '<div class="pp-root">' + summaryHtml + diffHtml + exportHtml + '</div>';

    /* ── Synchronized horizontal scroll ─────────────────────── */
    var sides = containerEl.querySelectorAll('.pp-diff-col');
    if (sides.length === 2) {
      var syncing = false;
      sides.forEach(function (side, idx) {
        side.addEventListener('scroll', function () {
          if (syncing) return;
          syncing = true;
          var other = sides[1 - idx];
          other.scrollTop = side.scrollTop;
          other.scrollLeft = side.scrollLeft;
          syncing = false;
        });
      });
    }

    /* ── Export handlers ─────────────────────────────────────── */
    containerEl.querySelector('[data-pp-export="csv"]').addEventListener('click', function () {
      downloadBlob(toCsv(standardized, cols), 'standardized.csv', 'text/csv');
    });
    containerEl.querySelector('[data-pp-export="json"]').addEventListener('click', function () {
      downloadBlob(JSON.stringify(standardized, null, 2), 'standardized.json', 'application/json');
    });
  }

  /* ── Public surface ─────────────────────────────────────────── */
  return { render: render };
})();

/* Export for module systems, no-op in plain browser */
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PreviewPanel;
}

