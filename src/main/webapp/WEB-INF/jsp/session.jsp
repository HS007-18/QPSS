<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Session #${session.id}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; color: #1e293b; min-height: 100vh; }
        .container { max-width: 1000px; margin: 0 auto; padding: 40px 20px; }
        .breadcrumb { font-size: 13px; color: #64748b; margin-bottom: 20px; }
        .breadcrumb a { color: #2563eb; text-decoration: none; }
        h1 { font-size: 24px; font-weight: 600; margin-bottom: 8px; color: #0f172a; }
        h2 { font-size: 18px; font-weight: 500; margin: 30px 0 16px; color: #334155; }
        .alert { padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; }
        .alert-success { background: #d1fae5; color: #065f46; border: 1px solid #10b981; }
        .alert-error { background: #fee2e2; color: #991b1b; border: 1px solid #ef4444; }
        .card { background: #ffffff; border-radius: 12px; padding: 24px; margin-bottom: 20px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.05); }
        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
        .stat { background: #ffffff; border-radius: 10px; padding: 16px; border: 1px solid #e2e8f0; text-align: center; }
        .stat .value { font-size: 28px; font-weight: 700; color: #2563eb; }
        .stat .label { font-size: 12px; color: #64748b; margin-top: 4px; }
        table { width: 100%; border-collapse: collapse; }
        th { text-align: left; padding: 10px 12px; font-size: 12px; color: #64748b; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
        td { padding: 10px 12px; font-size: 14px; border-bottom: 1px solid #f1f5f9; }
        tr:hover td { background: #f8fafc; }
        input, select, textarea { padding: 10px 14px; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 8px; color: #1e293b; font-size: 14px; outline: none; width: 100%; }
        input:focus, select:focus, textarea:focus { border-color: #2563eb; }
        textarea { resize: vertical; min-height: 80px; }
        button, .btn { padding: 10px 20px; background: #2563eb; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; text-decoration: none;}
        button:hover, .btn:hover { background: #1d4ed8; }
        .btn-sm { padding: 6px 14px; font-size: 12px; }
        .btn-danger { background: #dc2626; }
        .btn-danger:hover { background: #b91c1c; }
        .btn-green { background: #059669; }
        .btn-green:hover { background: #047857; }
        .form-grid { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 10px; margin-bottom: 12px; }
        .upload-zone { border: 2px dashed #cbd5e1; border-radius: 12px; padding: 30px; text-align: center; cursor: pointer; transition: border-color 0.2s; background: #f8fafc; }
        .upload-zone:hover { border-color: #2563eb; }
        .tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-right: 4px; }
        .tag-unit { background: #dbeafe; color: #1e40af; }
        .tag-co { background: #d1fae5; color: #065f46; }
        .tag-marks { background: #fef3c7; color: #92400e; }
        .actions-bar { display: flex; gap: 12px; margin-bottom: 20px; }
        img { max-width: 400px !important; height: auto !important; max-height: 250px !important; object-fit: contain; display: block; margin: 10px 0; }
    </style>
</head>
<body>
<div class="container">
    <div class="breadcrumb">
        <a href="/">Dashboard</a> / ${subject.name} / Session #${session.id}
    </div>
    <h1>${subject.name} — Session #${session.id}</h1>
    <div style="font-size:13px; color:#94a3b8; margin-bottom:20px;">Status: ${session.status}</div>

    <c:if test="${not empty message}">
        <div class="alert alert-success">${message}</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error">${error}</div>
    </c:if>
    <c:if test="${not empty shortages}">
        <div class="alert alert-error">
            <strong>Question bank insufficient:</strong><br>
            <c:forEach var="s" items="${shortages}">
                Unit ${s.unit} — ${s.marks}M: Need ${s.required}, have ${s.available} (${s.required - s.available} more needed)<br>
            </c:forEach>
        </div>
    </c:if>

    <h2>Upload Question Bank</h2>
    <div class="card">
        <form action="/sessions/${session.id}/upload" method="post" enctype="multipart/form-data">
            <div class="upload-zone" onclick="this.querySelector('input').click()">
                <input type="file" name="file" accept=".docx" required style="display:none"
                       onchange="this.closest('.upload-zone').querySelector('span').textContent = this.files[0].name">
                <span style="color:#64748b;">Click to select DOCX question bank</span>
            </div>
            <button type="submit" style="margin-top:12px;">Upload & Parse</button>
        </form>
    </div>



    <h2>Add Question Manually</h2>
    <div class="card">
        <form action="/sessions/${session.id}/questions" method="post">
            <div class="form-grid">
                <select name="unit" required>
                    <option value="">Unit</option>
                    <c:forEach var="u" begin="1" end="5">
                        <option value="${u}">Unit ${u}</option>
                    </c:forEach>
                </select>
                <select name="co" required>
                    <option value="">CO</option>
                    <c:forEach var="c" begin="1" end="5">
                        <option value="CO${c}">CO${c}</option>
                    </c:forEach>
                </select>
                <select name="marks" required>
                    <option value="">Marks</option>
                    <option value="2">2</option>
                    <option value="16">16</option>
                </select>
                <div></div>
            </div>
            <textarea name="content" placeholder="Question content..." required></textarea>
            <button type="submit" style="margin-top:10px;">Add Question</button>
        </form>
    </div>

    <h2 id="generate-section">Generate Question Paper</h2>
    <div class="card">
        <form action="/sessions/${session.id}/generate" method="post">
            <label style="display: block; font-size: 14px; color: #64748b; margin-bottom: 6px;">Exam Type</label>
            <select name="examType" required style="margin-bottom: 16px;">
                <option value="INTERNAL_1">Internal 1</option>
                <option value="INTERNAL_2">Internal 2</option>
                <option value="SEMESTER">Semester</option>
            </select>

            <label style="display: block; font-size: 14px; color: #64748b; margin-bottom: 6px;">Number of Sets</label>
            <input type="number" name="numSets" value="1" min="1" max="10" style="margin-bottom: 16px;">

            <button type="submit" class="btn-green" style="width: 100%; font-size: 16px; padding: 14px;">Generate Paper</button>
        </form>
    </div>
</div>
</body>
</html>
