<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KIT QGen - Error</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        body { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: var(--color-bg); padding: var(--space-4); }
        .error-card { background: var(--color-bg-elevated); border-radius: var(--radius-xl); padding: var(--space-10) var(--space-8); border: 1px solid var(--color-border); box-shadow: var(--shadow-lg); text-align: center; max-width: 520px; width: 100%; }
        .error-icon { display: inline-flex; align-items: center; justify-content: center; width: 80px; height: 80px; background: var(--color-danger-light); color: var(--color-danger); border-radius: 50%; margin-bottom: var(--space-6); }
        h1 { font-size: 1.5rem; font-weight: 700; margin-bottom: var(--space-3); color: var(--color-text); }
        p { font-size: 1rem; color: var(--color-text-secondary); margin-bottom: var(--space-8); line-height: 1.6; word-break: break-word; }
        .btn-container { display: flex; flex-direction: column; gap: var(--space-3); }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="error-icon" aria-hidden="true">
            <svg width="40" height="40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
        </div>
        <h1>Something Went Wrong</h1>
        <p><c:choose><c:when test="${not empty error}"><c:out value="${error}" /></c:when><c:otherwise>An unexpected error occurred. Please try again.</c:otherwise></c:choose></p>
        <div class="btn-container">
            <a href="/" class="btn btn-primary" style="padding: var(--space-3) var(--space-6);">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
                Return Home
            </a>
        </div>
    </div>
</body>
</html>