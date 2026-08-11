<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Paper Finalized</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; color: #1e293b; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
        .card { background: #ffffff; border-radius: 12px; padding: 40px; border: 1px solid #e2e8f0; box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1); text-align: center; max-width: 400px; width: 100%; }
        h1 { font-size: 24px; font-weight: 600; margin-bottom: 16px; color: #059669; }
        p { font-size: 15px; color: #64748b; margin-bottom: 30px; line-height: 1.5; }
        .btn-container { display: flex; flex-direction: column; gap: 12px; }
        .btn { padding: 14px 24px; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 15px; font-weight: 500; text-decoration: none; transition: background 0.2s; }
        .btn-primary { background: #0284c7; }
        .btn-primary:hover { background: #0369a1; }
        .btn-secondary { background: #64748b; }
        .btn-secondary:hover { background: #475569; }
    </style>
</head>
<body>
    <div class="card">
        <h1>🎉 Success!</h1>
        <p>The question paper has been successfully finalized and saved to the session history.</p>
        <div class="btn-container">
            <a href="/sessions/${session.id}/generate/export/${paperId}" target="_blank" class="btn btn-primary">Download PDF</a>
            <a href="/" class="btn btn-secondary">Cancel (Return to Dashboard)</a>
        </div>
    </div>
</body>
</html>
