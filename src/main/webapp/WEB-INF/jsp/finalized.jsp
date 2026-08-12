<%@ page contentType="text/html;charset=UTF-8" %>
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
    </style>
</head>
<body>
    <div class="success-card">
        <div class="success-icon">
            <svg style="width:40px;height:40px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
        </div>
        <h1>Successfully Finalized!</h1>
        <p>The question paper has been permanently saved to your session history and is ready for export.</p>
        
        <div class="btn-container">
            <a href="/sessions/${session.id}/generate/export/${paperId}" target="_blank" class="btn btn-primary" style="padding:14px;" onclick="setTimeout(() => { window.location.href = '/'; }, 500);">
                <svg style="width:20px;height:20px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                Download DOCX & Return Home
            </a>
            <a href="/" class="btn btn-outline" style="padding:14px;">Return Home without Downloading</a>
        </div>
    </div>
</body>
</html>
