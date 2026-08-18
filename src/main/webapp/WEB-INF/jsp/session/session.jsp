<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Session #${session.id}</title>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body>
<c:set var="pageTitle" value="Session #${session.id}" />
<c:set var="navRight"><span class="tag tag-unit" style="font-size:13px; font-weight:500;">Status: <c:out value="${session.status}" /></span></c:set>
<%@ include file="../fragments/navbar.jspf" %>

<div class="container">
    <div class="breadcrumb">
        <a href="/">Dashboard</a>
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        <c:out value="${subject.name}" />
        <svg style="width:14px;height:14px;margin-top:2px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        Session #${session.id}
    </div>

    <div class="page-header">
        <h1 class="page-title"><c:out value="${subject.name}" /></h1>
        <div class="page-subtitle">Build your question bank and generate question papers</div>
    </div>

    <c:if test="${not empty message}">
        <div class="card" style="background:var(--success-light); border-color:var(--success); color:#065f46; padding:16px;">
            <strong>Success:</strong> <c:out value="${message}" />
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="card" style="background:var(--danger-light); border-color:var(--danger); color:#991b1b; padding:16px;">
            <strong>Error:</strong> <c:out value="${error}" />
        </div>
    </c:if>
    <c:if test="${not empty shortages}">
        <div class="card" style="background:var(--danger-light); border-color:var(--danger); color:#991b1b; padding:16px;">
            <strong style="display:block; margin-bottom:8px;">Insufficient Question Bank:</strong>
            <ul style="margin-left:20px; font-size:14px;">
            <c:forEach var="s" items="${shortages}">
                <li>Unit ${s.unit} — ${s.marks}M: Need ${s.required}, have ${s.available} (${s.required - s.available} more needed)</li>
            </c:forEach>
            </ul>
        </div>
    </c:if>

    <div class="grid-2">

        <div>
            <h2 class="card-title" style="margin-bottom:16px; margin-top:0;">Question Bank Input</h2>

            <div class="card">
                <form action="/sessions/${session.id}/upload" method="post" enctype="multipart/form-data">
                    <label class="form-label">Upload DOCX File(s)</label>
                    <div class="upload-zone" onclick="this.querySelector('input').click()">
                        <div class="upload-icon">
                            <svg style="width:48px;height:48px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                        </div>
                        <input type="file" name="files" accept=".docx" multiple required style="display:none"
                               onchange="this.closest('.upload-zone').querySelector('span').textContent = this.files.length + ' file(s) selected'">
                        <span style="color:var(--text-muted); font-weight:500;">Click to select DOCX question bank files</span>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block" style="margin-top:16px;">Upload & Parse</button>
                </form>
            </div>

            <div class="card">
                <form action="/sessions/${session.id}/questions" method="post">
                    <label class="form-label">Add Question Manually</label>
                    <div class="grid-4" style="margin-bottom:12px;">
                        <select name="unit" required>
                            <option value="">Unit</option>
                            <c:forEach var="u" begin="1" end="5"><option value="${u}">U${u}</option></c:forEach>
                        </select>
                        <select name="rbt" required>
                            <option value="">RBT</option>
                            <option value="R">R</option>
                            <option value="U">U</option>
                            <option value="AP">AP</option>
                            <option value="AZ">AZ</option>
                        </select>
                        <select name="co" required>
                            <option value="">CO</option>
                            <c:forEach var="c" begin="1" end="5"><option value="CO${c}">CO${c}</option></c:forEach>
                        </select>
                        <select name="marks" required>
                            <option value="">Marks</option>
                            <option value="2">2</option>
                            <option value="16">16</option>
                            <option value="20">20</option>
                        </select>
                        <select name="t" required>
                            <option value="">I / II Half</option>
                            <option value="1">I</option>
                            <option value="2">II</option>
                        </select>
                    </div>
                    <textarea name="content" placeholder="Type question content here..." required style="margin-bottom:12px;"></textarea>
                    <button type="submit" class="btn btn-outline btn-block">Add Question</button>
                </form>
            </div>
        </div>

        <div>
            <h2 class="card-title" style="margin-bottom:16px; margin-top:0;">Generate Question Paper</h2>
            <div class="card">
                <form action="/sessions/${session.id}/generate" method="post">
                    <div class="form-group">
                        <label class="form-label">Exam Type</label>
                        <select name="examType" id="examType" required>
                            <option value="" disabled selected>Select Exam Type...</option>
                            <option value="INTERNAL_1">Internal 1</option>
                            <option value="INTERNAL_2">Internal 2</option>
                            <option value="SEMESTER">Semester</option>
                        </select>
                    </div>

                    <div id="dynamic-counts" style="display:none; gap: 16px; margin-bottom: 16px;">
                        <div class="form-group" style="flex:1; margin-bottom:0;">
                            <label class="form-label">Paper Format</label>
                            <select name="format" id="paperFormat">
                                <option value="FORMAT_1">Format 1 (10x2M, 5x16M)</option>
                                <option value="FORMAT_2">Format 2 (5x20M)</option>
                            </select>
                        </div>
                    </div>

                    <div id="preview-box" style="display:none; background: var(--bg-hover); padding: 16px; border-radius: 8px; font-size: 13px; font-family: monospace; margin-bottom: 24px; border: 1px solid var(--border); color:var(--text-main); white-space: pre-wrap;"></div>

                    <div class="form-group">
                        <label class="form-label">Number of Sets</label>
                        <input type="number" name="numSets" value="1" min="1" max="10">
                    </div>

                    <button type="submit" class="btn btn-accent btn-block" style="font-size: 16px; padding: 14px; margin-top:8px;">Generate Paper</button>
                </form>
            </div>
        </div>
    </div>

    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; margin-top:32px;">
        <h2 class="card-title" style="margin-bottom:0;">Current Question Bank</h2>
        <span class="tag tag-marks">${questions.size()} Questions</span>
    </div>
    <div class="card" style="padding:0; overflow:hidden;">
        <div class="table-responsive">
            <table>
                <thead>
                    <tr><th style="width:50px;">#</th><th>Details</th><th>T</th><th>Content</th><th>Source</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="q" items="${questions}" varStatus="i">
                        <tr>
                            <td style="color:var(--text-muted); font-weight:500;">${i.count}</td>
                            <td>
                                <div style="display:flex; gap:4px;">
                                    <span class="tag tag-unit">U${q.unit}</span>
                                    <span class="tag tag-rbt"><c:out value="${q.rbt}" /></span>
                                    <span class="tag tag-co"><c:out value="${q.co}" /></span>
                                    <span class="tag tag-marks">${q.marks}M</span>
                                </div>
                            </td>
                            <td style="font-size:13px; font-weight:600; color:var(--text-muted);">${q.t == 1 ? 'I' : (q.t == 2 ? 'II' : '')}</td>
                            <td><c:out value="${fn:substring(q.questionContent, 0, 80)}" /><c:if test="${fn:length(q.questionContent) > 80}">...</c:if></td>
                            <td style="font-size:13px; color:var(--text-muted);"><c:out value="${q.sourceFileName != null ? q.sourceFileName : 'Manual'}" /></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty questions}">
                        <tr>
                            <td colspan="5" style="text-align:center; padding:40px; color:var(--text-muted);">
                                No questions found. Upload a DOCX file or add questions manually.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    let isFetchingDefaults = false;

    function fetchPreview() {
        const type = document.getElementById('examType').value;
        const format = document.getElementById('paperFormat').value;
        const previewBox = document.getElementById('preview-box');

        if (!type) return;

        let url = '/sessions/${session.id}/generate/preview?examType=' + encodeURIComponent(type) + '&format=' + encodeURIComponent(format);

        fetch(url)
            .then(res => res.json())
            .then(data => {
                let html = '<strong>Distribution Plan Preview:</strong><br>';
                data.sections.forEach(sec => {
                    html += '<br><span style="color:var(--primary); font-weight:600;">' + sec.marks + ' Marks Section (Need ' + sec.totalRequired + ')</span><br>';
                    sec.units.forEach(u => {
                        html += ' - Unit ' + u.unit + ': ' + u.requiredCount + ' Qs ' +
                                '(T1: ' + u.t1Required + ', T2: ' + u.t2Required + ')<br>';
                    });
                });
                previewBox.innerHTML = html;
                previewBox.style.display = 'block';
                document.getElementById('dynamic-counts').style.display = 'flex';
                isFetchingDefaults = false;
            })
            .catch(err => {
                console.error(err);
                previewBox.innerHTML = 'Error loading preview.';
                previewBox.style.display = 'block';
            });
    }

    document.getElementById('examType').addEventListener('change', function(e) {
        isFetchingDefaults = true;
        fetchPreview();
    });

    document.getElementById('paperFormat').addEventListener('change', function(e) {
        fetchPreview();
    });
</script>
</body>
</html>