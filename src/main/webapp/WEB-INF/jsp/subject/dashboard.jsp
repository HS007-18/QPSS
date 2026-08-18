<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Dashboard</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<c:set var="pageTitle" value="Dashboard" />
<c:set var="navRight"><a href="/history" class="btn btn-outline btn-sm">View History</a></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container">
    <div class="page-header">
        <h1 class="page-title">Subjects</h1>
        <div class="page-subtitle">Manage your subjects and question banks</div>
    </div>

    <div class="card">
        <form action="/subjects" method="post" style="display:flex; gap:16px; align-items:flex-end;">
            <div class="form-group" style="flex:1; margin-bottom:0;">
                <label class="form-label">New Subject Name</label>
                <input type="text" name="name" placeholder="e.g. Data Structures" required>
            </div>
            <button type="submit" class="btn btn-primary">Create Subject</button>
        </form>
    </div>

    <c:if test="${empty subjects}">
        <div class="upload-zone" style="cursor:default; margin-top:24px;">
            <div style="font-size:48px; color:var(--border); margin-bottom:16px;">📚</div>
            <div style="font-size:16px; font-weight:500; color:var(--text-main);">No subjects yet</div>
            <div style="color:var(--text-muted); font-size:14px; margin-top:4px;">Create your first subject to start building question banks.</div>
        </div>
    </c:if>

    <div class="grid-3" style="margin-top:24px;">
        <c:forEach var="s" items="${subjects}">
            <div class="card" style="margin-bottom:0; display:flex; flex-direction:column;">
                <h3 style="font-size:18px; margin-bottom:8px;"><c:out value="${s.name}" /></h3>
                <div style="font-size:13px; color:var(--text-muted); margin-bottom:16px;">Created: <c:out value="${s.createdAt}" /></div>
                <div style="margin-top:auto; display:flex; gap:12px;">
                    <form action="/subjects/${s.id}/open" method="post" style="margin:0; flex:1;">
                        <button type="submit" class="btn btn-primary btn-sm" style="width:100%;">Open</button>
                    </form>
                    <form action="/subjects/${s.id}/delete" method="post" style="margin:0;">
                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Delete this subject?')">Delete</button>
                    </form>
                </div>
            </div>
        </c:forEach>
    </div>
</div>
</body>
</html>