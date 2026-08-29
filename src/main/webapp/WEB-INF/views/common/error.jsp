<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>KIT QGen - Error</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        body { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: var(--bg-hover); }
        .error-card { background: var(--bg-card); border-radius: 16px; padding: 48px 40px; border: 1px solid var(--danger); box-shadow: var(--shadow-lg); text-align: center; max-width: 520px; width: 100%; }
        .error-icon { display: inline-flex; align-items: center; justify-content: center; width: 80px; height: 80px; background: var(--danger-light); color: var(--danger); border-radius: 50%; margin-bottom: 24px; }
        h1 { font-size: 28px; font-weight: 700; margin-bottom: 16px; color: var(--text-main); }
        p { font-size: 15px; color: var(--text-muted); margin-bottom: 40px; line-height: 1.6; word-break: break-word; }
        .btn-container { display: flex; flex-direction: column; gap: 16px; }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="error-icon">
            <svg style="width:40px;height:40px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"></path></svg>
        </div>
        <h1>Something Went Wrong</h1>
        <p><c:choose><c:when test="${not empty error}"><c:out value="${error}" /></c:when><c:otherwise>An unexpected error occurred. Please try again.</c:otherwise></c:choose></p>
        <div class="btn-container">
            <a href="/" class="btn btn-primary" style="padding:14px;">Return Home</a>
        </div>
    </div>
</body>
</html>