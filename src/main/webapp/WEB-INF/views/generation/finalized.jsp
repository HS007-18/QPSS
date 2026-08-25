<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Paper Finalized</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        body { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: var(--bg-hover); }
        .success-card { background: var(--bg-card); border-radius: 16px; padding: 48px 40px; border: 1px solid var(--border); box-shadow: var(--shadow-lg); text-align: center; max-width: 480px; width: 100%; }
        .success-icon { display: inline-flex; align-items: center; justify-content: center; width: 80px; height: 80px; background: var(--success-light); color: var(--success); border-radius: 50%; margin-bottom: 24px; }
        h1 { font-size: 28px; font-weight: 700; margin-bottom: 16px; color: var(--text-main); }
        p { font-size: 15px; color: var(--text-muted); margin-bottom: 40px; line-height: 1.6; }
        .btn-container { display: flex; flex-direction: column; gap: 16px; }
        .download-status { font-size: 13px; color: var(--success); font-weight: 600; margin-bottom: 16px; }
        .countdown { font-size: 13px; color: var(--text-muted); margin-top: 8px; }
    </style>
</head>
<body>
    <div class="success-card">
        <div class="success-icon">
            <svg style="width:40px;height:40px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
        </div>
        <h1>Successfully Finalized!</h1>
        <p>The question paper has been permanently saved and is downloading now.</p>
        <div class="download-status" id="downloadStatus">⬇ Starting download...</div>
        <div class="countdown" id="countdown">Redirecting to Dashboard in <strong>5</strong> seconds...</div>

        <div class="btn-container">
            <a href="/sessions/${session.id}/generate/export/${paperId}" class="btn btn-primary" style="padding:14px;" id="downloadAgainBtn">Download Again</a>
            <a href="/" class="btn btn-outline" style="padding:14px;">Return to Dashboard Now</a>
        </div>
    </div>

<script>
    // Prevent back-button navigating to stale review/generation pages
    history.replaceState(null, '', location.href);
    window.addEventListener('popstate', function() {
        // When back is pressed, redirect to dashboard instead
        window.location.replace('/');
    });

    // Auto-download the file
    (function() {
        var downloadUrl = '/sessions/${session.id}/generate/export/${paperId}';
        var iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = downloadUrl;
        document.body.appendChild(iframe);
        document.getElementById('downloadStatus').textContent = '✓ Download started!';
    })();

    // Countdown redirect to dashboard
    var seconds = 5;
    var countdownEl = document.getElementById('countdown');
    var interval = setInterval(function() {
        seconds--;
        if (seconds <= 0) {
            clearInterval(interval);
            window.location.replace('/');
        } else {
            countdownEl.innerHTML = 'Redirecting to Dashboard in <strong>' + seconds + '</strong> seconds...';
        }
    }, 1000);
</script>
</body>
</html>
