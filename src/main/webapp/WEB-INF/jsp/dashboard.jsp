<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; color: #1e293b; min-height: 100vh; }
        .container { max-width: 900px; margin: 0 auto; padding: 40px 20px; }
        h1 { font-size: 28px; font-weight: 600; margin-bottom: 30px; color: #0f172a; }
        .card { background: #ffffff; border-radius: 12px; padding: 24px; margin-bottom: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.05); transition: border-color 0.2s; }
        .card:hover { border-color: #2563eb; }
        .card h3 { font-size: 18px; color: #0f172a; margin-bottom: 6px; }
        .card .meta { font-size: 13px; color: #64748b; }
        .card a { color: #2563eb; text-decoration: none; font-weight: 500; }
        .card a:hover { text-decoration: underline; }
        .form-row { display: flex; gap: 12px; margin-bottom: 30px; }
        input[type="text"] { flex: 1; padding: 12px 16px; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; color: #1e293b; font-size: 15px; outline: none; }
        input[type="text"]:focus { border-color: #2563eb; }
        button, .btn { padding: 12px 24px; background: #2563eb; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; text-decoration: none;}
        button:hover, .btn:hover { background: #1d4ed8; }
        .empty { text-align: center; padding: 60px; color: #64748b; font-size: 15px; }
        .badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 12px; background: #dbeafe; color: #1e40af; }
        .actions { display: flex; gap: 12px; align-items: center; margin-top: 10px; }
        .btn-sm { padding: 6px 14px; font-size: 12px; border-radius: 6px; }
        .btn-danger { background: #dc2626; }
        .btn-danger:hover { background: #b91c1c; }
    </style>
</head>
<body>
<div class="container">
    <h1>Question Paper Selection System</h1>

    <form action="/subjects" method="post" class="form-row">
        <input type="text" name="name" placeholder="New subject name..." required>
        <button type="submit">Create Subject</button>
    </form>

    <c:if test="${empty subjects}">
        <div class="empty">No subjects yet. Create one to get started.</div>
    </c:if>

    <c:forEach var="s" items="${subjects}">
        <div class="card">
            <h3><a href="/subjects/${s.id}">${s.name}</a></h3>
            <div class="meta">Created: ${s.createdAt}</div>
            <div class="actions">
                <a href="/subjects/${s.id}" class="btn btn-sm">Open</a>
                <form action="/subjects/${s.id}/delete" method="post" style="margin:0;">
                    <button type="submit" class="btn btn-sm btn-danger"
                            onclick="return confirm('Delete this subject?')">Delete</button>
                </form>
            </div>
        </div>
    </c:forEach>
</div>
</body>
</html>
