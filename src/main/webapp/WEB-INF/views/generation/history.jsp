<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>KIT QGen - Paper History</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<c:set var="pageTitle" value="History" />
<c:set var="navRight"><span class="tag tag-unit" style="font-size:13px; font-weight:500;">History</span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="max-width: 1000px;">
    <div class="breadcrumb">
        <a href="/">Dashboard</a>
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        History
    </div>

    <div class="page-header">
        <h1 class="page-title">Paper History</h1>
        <div class="page-subtitle">View all finalized question papers</div>
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
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="p" items="${papers}">
                        <tr>
                            <td style="color:var(--text-muted); font-weight:500;">#${p.id}</td>
                            <td style="font-weight:500;"><c:out value="${p.examType.replace('_', ' ')}" /></td>
                            <td><span class="tag tag-unit" style="background:var(--accent-light); color:var(--accent-hover);">Set <c:out value="${p.setLabel}" /></span></td>
                            <td style="color:var(--text-muted);">Session #${p.sessionId}</td>
                            <td style="color:var(--text-muted);">Sub #${p.subjectId}</td>
                            <td>
                                <a href="/sessions/${p.sessionId}/generate/export/${p.id}" class="btn btn-outline btn-sm">Download</a>
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