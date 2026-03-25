/**
 * PreviewPanel — reusable before/after diff component.
 * Zero dependencies · vanilla JS · CSS injected once.
 *
 * Usage:
 *   PreviewPanel.render(containerEl, {
 *     original:       [ {col:val, …}, … ],
 *     standardized:   [ {col:val, …}, … ],
 *     changedFields:  { "fieldName": "Rule Name", … }  // optional
 *     elapsedMs:      42,                                // optional
 *     qualityReport:  { overallStatus, qualityScore,     // optional
 *                       totalRecords, ruleResults: [] }
 *   }, {
 *     onSkipQualityCheck: function () { … }              // optional callback
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
      '.pp-btn:disabled{opacity:.45;cursor:not-allowed}',
      '.pp-btn:disabled:hover{color:var(--color-text-dim,#8b90a0);border-color:var(--color-border,#2a2f3e)}',
      '.pp-empty{text-align:center;padding:32px;color:var(--color-text-dim,#8b90a0)}',

      /* Quality summary strip — shared */
      '.pp-quality-strip{padding:12px 16px;border-radius:0;margin-bottom:14px;font-size:.78rem;line-height:1.5}',

      /* PASS */
      '.pp-quality-strip.pp-qpass{border-left:3px solid #3B6D11;background:#EAF3DE;color:#2c5a0e}',

      /* WARN */
      '.pp-quality-strip.pp-qwarn{border-left:3px solid #854F0B;background:#FAEEDA;color:#6b3f09}',
      '.pp-qwarn-toggle{background:none;border:none;color:#854F0B;font-size:.74rem;cursor:pointer;text-decoration:underline;padding:0;margin-top:4px;font-family:inherit}',
      '.pp-qwarn-details{margin-top:6px;padding-left:12px;font-size:.74rem;line-height:1.6}',

      /* FAIL */
      '.pp-quality-strip.pp-qfail{background:#FCEBEB;color:#991B1B;padding:20px 16px}',
      '.pp-qfail h3{margin:0 0 10px;font-size:.9rem;font-weight:700;color:#B91C1C}',
      '.pp-qfail-card{background:rgba(153,27,27,.06);border:1px solid rgba(153,27,27,.15);border-radius:4px;padding:8px 12px;margin-bottom:6px;font-size:.74rem;line-height:1.5}',
      '.pp-qfail-card strong{color:#991B1B}',
      '.pp-qfail-msg{color:#7F1D1D;font-style:italic;margin-top:2px}',
      '.pp-qfail-resubmit{margin-top:14px;padding:8px 18px;border:1px solid #B91C1C;border-radius:6px;background:#B91C1C;color:#fff;font-size:.76rem;font-weight:600;cursor:pointer;font-family:inherit}',
      '.pp-qfail-resubmit:disabled{opacity:.45;cursor:not-allowed}',

      /* Column health dots */
      '.pp-col-dot{display:inline-block;width:8px;height:8px;border-radius:50%;margin-right:4px;vertical-align:middle}',
      '.pp-col-dot.pp-dot-error{background:#DC2626}',
      '.pp-col-dot.pp-dot-warn{background:#D97706}',

      /* Export warning badge */
      '.pp-export-warn{color:#D97706;font-size:.68rem;margin-left:4px}',

      /* Quality score in summary strip */
      '.pp-qscore-pass{color:#3B6D11;font-weight:700}',
      '.pp-qscore-warn{color:#D97706;font-weight:700}'
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

  /**
   * Build a per-column health map from qualityReport.ruleResults.
   * Returns { columnName: { severity: "ERROR"|"WARNING", tooltip: "..." } }
   * ERROR takes precedence over WARNING for any given column.
   */
  function buildColumnHealthMap(qualityReport) {
    var map = {};
    if (!qualityReport || !qualityReport.ruleResults) return map;
    qualityReport.ruleResults.forEach(function (r) {
      var col = r.columnName;
      if (!col) return;
      var sev = (r.severity || r.status || '').toUpperCase();
      if (sev !== 'ERROR' && sev !== 'WARNING') return;
      var existing = map[col];
      // ERROR trumps WARNING
      if (!existing || (sev === 'ERROR' && existing.severity !== 'ERROR')) {
        var pct = r.failRatePct != null ? r.failRatePct : '?';
        map[col] = {
          severity: sev,
          tooltip: col + ': ' + (r.ruleName || 'rule') + ' failed \u2014 ' + pct + '% of values'
        };
      }
    });
    return map;
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

  /* ── Quality strip builders ─────────────────────────────────── */
  function buildQualityPassHtml(qr) {
    return '<div class="pp-quality-strip pp-qpass">' +
      'Quality check passed &mdash; score ' + esc(qr.qualityScore) +
      ' &middot; ' + esc(qr.totalRecords) + ' records' +
      '</div>';
  }

  function buildQualityWarnHtml(qr) {
    var warnings = (qr.ruleResults || []).filter(function (r) {
      return (r.severity || r.status || '').toUpperCase() === 'WARNING';
    });
    var html = '<div class="pp-quality-strip pp-qwarn">';
    html += 'Quality warnings &mdash; score ' + esc(qr.qualityScore);
    if (warnings.length) {
      html += '<br><button class="pp-qwarn-toggle" data-pp-warn-toggle>Show warnings</button>';
      html += '<div class="pp-qwarn-details" data-pp-warn-details style="display:none">';
      warnings.forEach(function (r) {
        html += '<div>' + esc(r.ruleName) + ' &middot; ' + esc(r.columnName) +
          ' &middot; ' + esc(r.failRatePct) + '% fail</div>';
      });
      html += '</div>';
    }
    html += '</div>';
    return html;
  }

  function buildQualityFailHtml(qr, hasCallback) {
    var errors = (qr.ruleResults || []).filter(function (r) {
      return (r.severity || r.status || '').toUpperCase() === 'ERROR';
    });
    var html = '<div class="pp-quality-strip pp-qfail">';
    html += '<h3>Preview blocked &mdash; quality check failed (score ' + esc(qr.qualityScore) + ')</h3>';
    errors.forEach(function (r) {
      html += '<div class="pp-qfail-card">';
      html += '<strong>' + esc(r.ruleName) + '</strong> &middot; ' + esc(r.columnName) +
        ' &middot; ' + esc(r.failRatePct) + '% fail &middot; ' + esc(r.failCount) + ' failures';
      if (r.message) {
        html += '<div class="pp-qfail-msg">' + esc(r.message) + '</div>';
      }
      html += '</div>';
    });
    if (hasCallback) {
      html += '<button class="pp-qfail-resubmit" data-pp-resubmit>Resubmit without quality check</button>';
    } else {
      html += '<button class="pp-qfail-resubmit" data-pp-resubmit disabled title="Not available in this context.">Resubmit without quality check</button>';
    }
    html += '</div>';
    return html;
  }

  /* ── Public API ─────────────────────────────────────────────── */
  function render(containerEl, opts, renderOpts) {
    injectStyles();
    if (!containerEl) return;

    renderOpts = renderOpts || {};
    var onSkipQualityCheck = typeof renderOpts.onSkipQualityCheck === 'function'
      ? renderOpts.onSkipQualityCheck : null;

    var original     = opts.original || [];
    var standardized = opts.standardized || [];
    var changedFields = opts.changedFields || {};
    var elapsedMs    = opts.elapsedMs;
    var qualityReport = opts.qualityReport || null;

    if (!original.length && !standardized.length) {
      containerEl.innerHTML = '<div class="pp-empty">No data to preview.</div>';
      return;
    }

    var cols = collectCols(original, standardized);
    var stats = computeChanges(original, standardized, cols);
    var qStatus = qualityReport ? (qualityReport.overallStatus || '').toUpperCase() : null;
    var isFail = qStatus === 'FAIL';
    var isWarn = qStatus === 'WARN';
    var isPass = qStatus === 'PASS';

    /* ── Quality strip (above diff table) ─────────────────────── */
    var qualityHtml = '';
    if (qualityReport) {
      if (isFail)      qualityHtml = buildQualityFailHtml(qualityReport, !!onSkipQualityCheck);
      else if (isWarn)  qualityHtml = buildQualityWarnHtml(qualityReport);
      else if (isPass)  qualityHtml = buildQualityPassHtml(qualityReport);
    }

    /* ── Summary strip ──────────────────────────────────────── */
    var summaryHtml =
      '<div class="pp-summary">' +
        '<span><span class="pp-stat">' + stats.recordsChanged + '</span> of ' + original.length + ' records changed</span>' +
        '<span><span class="pp-stat">' + stats.fieldsChanged + '</span> fields affected</span>' +
        (elapsedMs != null ? '<span><span class="pp-stat">' + elapsedMs + '</span>ms</span>' : '');

    // Quality score suffix (not shown when FAIL — banner replaces everything)
    if (qualityReport && !isFail) {
      var scoreClass = isPass ? 'pp-qscore-pass' : 'pp-qscore-warn';
      summaryHtml += '<span>quality score: <span class="' + scoreClass + '">' +
        esc(qualityReport.qualityScore) + '</span></span>';
    }
    summaryHtml += '</div>';

    /* ── Column health map ─────────────────────────────────── */
    var colHealth = buildColumnHealthMap(qualityReport);

    /* ── Build diff tables ──────────────────────────────────── */
    function buildTableHtml(records, labelClass, labelText, isStandardized) {
      var html = '<div class="pp-diff-col" data-side="' + labelClass + '">';
      html += '<div class="pp-diff-label ' + labelClass + '">' + labelText + '</div>';
      html += '<table class="pp-diff-table"><thead><tr>';
      cols.forEach(function (c) {
        var dotHtml = '';
        if (colHealth[c]) {
          var dotClass = colHealth[c].severity === 'ERROR' ? 'pp-dot-error' : 'pp-dot-warn';
          dotHtml = '<span class="pp-col-dot ' + dotClass + '" title="' + esc(colHealth[c].tooltip) + '"></span>';
        }
        html += '<th>' + dotHtml + esc(c) + '</th>';
      });
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

    /* If FAIL — hide diff table entirely, show only blocked banner */
    var diffHtml = '';
    if (!isFail) {
      diffHtml =
        '<div class="pp-diff-wrap">' +
          buildTableHtml(original, 'original', 'Original', false) +
          buildTableHtml(standardized, 'standardized', 'Standardized', true) +
        '</div>';
    }

    /* ── Export bar ──────────────────────────────────────────── */
    var exportHtml = '';
    if (isFail) {
      var disabledTip = 'Export disabled — quality check failed. Resubmit with skipQualityCheck to proceed.';
      exportHtml =
        '<div class="pp-export-bar">' +
          '<button class="pp-btn" data-pp-export="csv" disabled title="' + esc(disabledTip) + '">⬇ Export CSV</button>' +
          '<button class="pp-btn" data-pp-export="json" disabled title="' + esc(disabledTip) + '">⬇ Export JSON</button>' +
        '</div>';
    } else if (isWarn) {
      exportHtml =
        '<div class="pp-export-bar">' +
          '<button class="pp-btn" data-pp-export="csv">⬇ Export CSV<span class="pp-export-warn">(\u26A0 quality warnings)</span></button>' +
          '<button class="pp-btn" data-pp-export="json">⬇ Export JSON<span class="pp-export-warn">(\u26A0 quality warnings)</span></button>' +
        '</div>';
    } else {
      exportHtml =
        '<div class="pp-export-bar">' +
          '<button class="pp-btn" data-pp-export="csv">⬇ Export CSV</button>' +
          '<button class="pp-btn" data-pp-export="json">⬇ Export JSON</button>' +
        '</div>';
    }

    /* When FAIL the summary is not shown — only the blocked banner + export bar */
    var bodyHtml = isFail
      ? qualityHtml + exportHtml
      : qualityHtml + summaryHtml + diffHtml + exportHtml;

    containerEl.innerHTML = '<div class="pp-root">' + bodyHtml + '</div>';

    /* ── Warn toggle ──────────────────────────────────────────── */
    var warnToggle = containerEl.querySelector('[data-pp-warn-toggle]');
    if (warnToggle) {
      warnToggle.addEventListener('click', function () {
        var details = containerEl.querySelector('[data-pp-warn-details]');
        if (!details) return;
        var hidden = details.style.display === 'none';
        details.style.display = hidden ? 'block' : 'none';
        warnToggle.textContent = hidden ? 'Hide warnings' : 'Show warnings';
      });
    }

    /* ── Resubmit button ─────────────────────────────────────── */
    var resubmitBtn = containerEl.querySelector('[data-pp-resubmit]');
    if (resubmitBtn && onSkipQualityCheck) {
      resubmitBtn.addEventListener('click', function () {
        onSkipQualityCheck();
      });
    }

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

    /* ── Export handlers (only when not disabled) ─────────────── */
    var csvBtn = containerEl.querySelector('[data-pp-export="csv"]');
    var jsonBtn = containerEl.querySelector('[data-pp-export="json"]');
    if (csvBtn && !csvBtn.disabled) {
      csvBtn.addEventListener('click', function () {
        downloadBlob(toCsv(standardized, cols), 'standardized.csv', 'text/csv');
      });
    }
    if (jsonBtn && !jsonBtn.disabled) {
      jsonBtn.addEventListener('click', function () {
        downloadBlob(JSON.stringify(standardized, null, 2), 'standardized.json', 'application/json');
      });
    }
  }

  /* ── Public surface ─────────────────────────────────────────── */
  return { render: render };
})();

/* Export for module systems, no-op in plain browser */
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PreviewPanel;
}

