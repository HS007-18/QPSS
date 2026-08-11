<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>QPSS - Review Generated Papers</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; color: #1e293b; min-height: 100vh; }
        .container { max-width: 1100px; margin: 0 auto; padding: 40px 20px; }
        .breadcrumb { font-size: 13px; color: #64748b; margin-bottom: 20px; }
        .breadcrumb a { color: #2563eb; text-decoration: none; }
        h1 { font-size: 24px; margin-bottom: 8px; color: #0f172a; }
        h2 { font-size: 18px; margin: 24px 0 12px; color: #334155; }
        .warning { background: #fef3c7; color: #92400e; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; border: 1px solid #f59e0b; }
        .set-card { background: #ffffff; border-radius: 12px; padding: 24px; margin-bottom: 24px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.05); }
        .set-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
        .set-label { font-size: 20px; font-weight: 600; color: #2563eb; }
        .section-title { font-size: 14px; font-weight: 600; color: #64748b; text-transform: uppercase; margin: 16px 0 8px; padding-bottom: 6px; border-bottom: 1px solid #e2e8f0; }
        .question { padding: 8px 0; font-size: 14px; border-bottom: 1px solid #f1f5f9; }
        .question .num { color: #2563eb; font-weight: 600; min-width: 30px; display: inline-block; }
        .question .marks-tag { float: right; font-size: 11px; color: #92400e; background: #fef3c7; padding: 2px 8px; border-radius: 4px; }
        .or-divider { text-align: center; color: #64748b; font-size: 12px; padding: 4px 0; font-style: italic; }
        .pair-block { background: #f8fafc; border-radius: 8px; padding: 12px 16px; margin-bottom: 8px; border: 1px solid #e2e8f0; }
        button, .btn { padding: 10px 20px; background: #2563eb; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; text-decoration: none;}
        button:hover, .btn:hover { background: #1d4ed8; }
        .btn-green { background: #059669; }
        .btn-green:hover { background: #047857; }
        .unit-tag { font-size: 11px; color: #1e40af; background: #dbeafe; padding: 2px 6px; border-radius: 4px; margin-left: 8px; }
        img { max-width: 400px !important; height: auto !important; max-height: 250px !important; object-fit: contain; display: block; margin: 10px 0; }
    </style>
</head>
<body>
<div class="container">
    <div class="breadcrumb">
        <a href="/">Dashboard</a> / ${subject.name} /
        <a href="/sessions/${session.id}">Session #${session.id}</a> / Review
    </div>
    <h1>Generated Papers — ${examType.replace('_', ' ')}</h1>
    <div style="font-size:13px; color:#94a3b8; margin-bottom:20px;">${subject.name}</div>

    <c:if test="${not empty result.diversityWarning}">
        <div class="warning">${result.diversityWarning}</div>
    </c:if>

    <c:forEach var="set" items="${result.sets}">
        <div class="set-card">
            <div class="set-header" style="align-items: flex-start;">
                <span class="set-label">Set ${set.paper.setLabel}</span>
                <div style="display:flex; flex-direction:column; gap: 10px; align-items: flex-end;">
                    <a href="/sessions/${session.id}#generate-section" class="btn" style="background:#64748b; text-decoration:none; padding: 6px 12px; font-size: 13px;">Regenerate Papers</a>
                    <div style="display:flex; gap: 10px;">
                        <a href="/sessions/${session.id}/generate/export/${set.paper.id}" target="_blank" class="btn" style="background:#0284c7; text-decoration:none;">Download PDF</a>
                        <form action="/sessions/${session.id}/generate/${set.paper.id}/finalize" method="post" style="margin:0;">
                            <button type="submit" class="btn btn-green">Finalize This Set</button>
                        </form>
                    </div>
                </div>
            </div>

            <div class="section-title">Section A — 2 Marks (10 × 2 = 20 Marks)</div>
            <c:forEach var="q" items="${set.sectionA}" varStatus="i">
                <div class="question">
                    <span class="num">${i.count}.</span>
                    ${q.questionContent}
                    <span class="marks-tag">2M</span>
                    <span class="unit-tag">U${q.unit}</span>
                </div>
            </c:forEach>

            <div class="section-title">Section B — 16 Marks (5 × 16 = 80 Marks)</div>
            <c:forEach var="pair" items="${set.sectionB}">
                <div class="pair-block">
                    <div class="question">
                        <span class="num">${10 + pair.pairIndex}. (a)</span>
                        ${pair.choiceA.questionContent}
                        <span class="marks-tag">16M</span>
                        <span class="unit-tag">U${pair.unit}</span>
                    </div>
                    <div class="or-divider">— OR —</div>
                    <div class="question">
                        <span class="num">${10 + pair.pairIndex}. (b)</span>
                        ${pair.choiceB.questionContent}
                        <span class="marks-tag">16M</span>
                        <span class="unit-tag">U${pair.unit}</span>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:forEach>

    <a href="/sessions/${session.id}" class="btn" style="background:#334155;">Back to Session</a>
</div>
</body>
</html>
