<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Fix Upload Issue</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .original-text { background: var(--bg-hover); padding: 12px; border-radius: 8px; font-size: 14px; color: var(--text-muted); margin-bottom: 20px; word-break: break-word; border: 1px solid var(--border); }
    </style>
</head>
<body>
<nav class="navbar">
    <a href="/" class="navbar-brand">
        <svg style="width:24px;height:24px;color:var(--accent);" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9.5a2.5 2.5 0 00-2.5-2.5H15"></path></svg>
        QPSS <span>Dashboard</span>
    </a>
    <div style="margin-left:auto;">
        <span class="tag tag-unit" style="font-size:13px; font-weight:500;">Action Required</span>
    </div>
</nav>

<div class="container" style="max-width: 650px;">
    <div class="page-header">
        <h1 class="page-title">Missing Data</h1>
        <div class="page-subtitle">Please provide the missing information for a question uploaded from <strong>${file}</strong>.</div>
    </div>

    <div class="card">
        <form action="/sessions/${session.id}/upload/fix" method="post">
            <input type="hidden" name="file" value="${file}" />
            <input type="hidden" name="serialNo" value="${question.serialNo}" />

            <c:if test="${empty question.questionContent}">
                <div class="form-group">
                    <label class="form-label">Question S.No ${question.serialNo} Content:</label>
                    <textarea name="questionContent" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required placeholder="Enter missing question content..."></textarea>
                </div>
            </c:if>
            <c:if test="${not empty question.questionContent}">
                <div class="form-group">
                    <label class="form-label">Question S.No ${question.serialNo} Content:</label>
                    <div class="original-text">${question.questionContent}</div>
                    <input type="hidden" name="questionContent" value="${question.questionContent}" />
                </div>
            </c:if>

            <c:if test="${empty question.unit}">
                <div class="form-group">
                    <label class="form-label">Unit (1-5):</label>
                    <select name="unit" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required>
                        <option value="" disabled selected>Select missing unit...</option>
                        <c:forEach var="u" begin="1" end="5"><option value="${u}">Unit ${u}</option></c:forEach>
                    </select>
                </div>
            </c:if>
            <c:if test="${not empty question.unit}">
                <input type="hidden" name="unit" value="${question.unit}" />
            </c:if>

            <c:if test="${empty question.co}">
                <div class="form-group">
                    <label class="form-label">CO (e.g., CO1):</label>
                    <input type="text" name="co" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" placeholder="Enter missing CO (e.g., CO1)" required />
                </div>
            </c:if>
            <c:if test="${not empty question.co}">
                <div class="form-group">
                    <label class="form-label">CO:</label>
                    <div class="original-text">${question.co}</div>
                    <input type="hidden" name="co" value="${question.co}" />
                </div>
            </c:if>

            <c:if test="${empty question.marks}">
                <div class="form-group">
                    <label class="form-label">Marks (2, 16 or 20):</label>
                    <select name="marks" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required>
                        <option value="" disabled selected>Select missing marks...</option>
                        <option value="2">2</option>
                        <option value="16">16</option>
                        <option value="20">20</option>
                    </select>
                </div>
            </c:if>
            <c:if test="${not empty question.marks}">
                <div class="form-group">
                    <label class="form-label">Marks:</label>
                    <div class="original-text">${question.marks}</div>
                    <input type="hidden" name="marks" value="${question.marks}" />
                </div>
            </c:if>

            <c:if test="${empty question.t}">
                <div class="form-group">
                    <label class="form-label">I / II Half (1=First Half, 2=Second Half):</label>
                    <select name="t" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required>
                        <option value="" disabled selected>Select missing I / II Half value...</option>
                        <option value="1">1 (First Half)</option>
                        <option value="2">2 (Second Half)</option>
                    </select>
                </div>
            </c:if>
            <c:if test="${not empty question.t}">
                <div class="form-group">
                    <label class="form-label">I / II Half:</label>
                    <div class="original-text">${question.t == 1 ? '1 (First Half)' : '2 (Second Half)'}</div>
                    <input type="hidden" name="t" value="${question.t}" />
                </div>
            </c:if>

            <c:if test="${empty question.rbt}">
                <div class="form-group">
                    <label class="form-label">RBT Level (R, U, AP, AZ):</label>
                    <select name="rbt" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required>
                        <option value="" disabled selected>Select missing RBT level...</option>
                        <option value="R">R - Remember</option>
                        <option value="U">U - Understand</option>
                        <option value="AP">AP - Apply</option>
                        <option value="AZ">AZ - Analyse</option>
                    </select>
                </div>
            </c:if>
            <c:if test="${not empty question.rbt}">
                <div class="form-group">
                    <label class="form-label">RBT Level:</label>
                    <div class="original-text">${question.rbt}</div>
                    <input type="hidden" name="rbt" value="${question.rbt}" />
                </div>
            </c:if>

            <button type="submit" class="btn btn-primary btn-block" style="font-size:16px; padding:14px; margin-top:24px;">Save & Continue</button>
        </form>
    </div>
</div>
</body>
</html>

