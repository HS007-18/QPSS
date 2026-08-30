<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>KIT QGen - Fix Upload Issue</title>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .original-text { background: var(--bg-hover); padding: 12px; border-radius: 8px; font-size: 14px; color: var(--text-muted); margin-bottom: 20px; word-break: break-word; border: 1px solid var(--border); }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Fix Upload" />
<c:set var="navRight"><span class="tag tag-unit" style="font-size:13px; font-weight:500;">Action Required</span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container" style="max-width: 650px;">
    <div class="page-header">
        <h1 class="page-title">Missing Data</h1>
        <div class="page-subtitle">Please provide the missing information for a question uploaded from <strong><c:out value="${file}" /></strong>.</div>
    </div>

    <div class="card">
        <form action="/sessions/${session.id}/upload/fix" method="post">
            <input type="hidden" name="file" value="<c:out value='${file}' />" />
            <input type="hidden" name="fileIndex" value="${fileIndex}" />
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
                    <div class="original-text"><c:out value="${question.questionContent}" /></div>
                    <input type="hidden" name="questionContent" value="<c:out value='${question.questionContent}' />" />
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
                    <div class="original-text"><c:out value="${question.co}" /></div>
                    <input type="hidden" name="co" value="<c:out value='${question.co}' />" />
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
                    <label class="form-label">RBT Level (R, U, AP, AZ, E, C):</label>
                    <select name="rbt" style="border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-light);" required>
                        <option value="" disabled selected>Select missing RBT level...</option>
                        <option value="R">R - Remember</option>
                        <option value="U">U - Understand</option>
                        <option value="AP">AP - Apply</option>
                        <option value="AZ">AZ - Analyse</option>
                        <option value="E">E - Evaluate</option>
                        <option value="C">C - Create</option>
                    </select>
                </div>
            </c:if>
            <c:if test="${not empty question.rbt}">
                <div class="form-group">
                    <label class="form-label">RBT Level:</label>
                    <div class="original-text"><c:out value="${question.rbt}" /></div>
                    <input type="hidden" name="rbt" value="<c:out value='${question.rbt}' />" />
                </div>
            </c:if>

            <button type="submit" class="btn btn-primary btn-block" style="font-size:16px; padding:14px; margin-top:24px;">Save & Continue</button>
        </form>
    </div>
</div>
</body>
</html>