<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Paper History</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<nav class="navbar">
    <a href="/" class="navbar-brand">
        <svg style="width:24px;height:24px;color:var(--accent);" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2.5 2.5 0 00-2.5-2.5H15"></path></svg>
        QPSS <span>Dashboard</span>
    </a>
    <div style="margin-left:auto;">
        <span class="tag tag-unit" style="font-size:13px; font-weight:500;">History</span>
    </div>
</nav>

<div class="container" style="max-width: 1000px;">
    <div class="breadcrumb">
        <a href="/">Dashboard</a> 
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg> 
        History
    </div>
    
    <div class="page-header">
        <h1 class="page-title">Paper History</h1>
        <div class="page-subtitle">View and download all finalized question papers</div>
    </div>
    
    <div class="card" style="padding:0; overflow:hidden;">
        <div class="table-responsive">
            <table>
                <thead>
                    <tr>
                        <th style="width:100px;">Paper ID</th>
                        <th>Exam Type</th>
                        <th>Set Label</th>
                        <th>Session ID</th>
                        <th>Subject ID</th>
                        <th style="text-align:right;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="p" items="${papers}">
                        <tr>
                            <td style="color:var(--text-muted); font-weight:500;">#${p.id}</td>
                            <td style="font-weight:500;">${p.examType.replace('_', ' ')}</td>
                            <td><span class="tag tag-unit" style="background:var(--accent-light); color:var(--accent-hover);">Set ${p.setLabel}</span></td>
                            <td style="color:var(--text-muted);">Session #${p.sessionId}</td>
                            <td style="color:var(--text-muted);">Sub #${p.subjectId}</td>
                            <td style="text-align:right;">
                                <a href="/sessions/${p.sessionId}/generate/export/${p.id}" target="_blank" class="btn btn-primary btn-sm" style="display:inline-flex;">
                                    <svg style="width:16px;height:16px;margin-right:4px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                                    Download
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty papers}">
                        <tr>
                            <td colspan="6" style="text-align: center; padding: 40px; color: var(--text-muted);">
                                No finalized papers found in your history.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
