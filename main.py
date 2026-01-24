from typing import Optional, List, Dict, Any

from fastapi import FastAPI, Query, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.responses import FileResponse

from Web_app.database import (
    init_db,
    list_cves,
    list_cves_paged,
    count_cves,
    get_cve,
    count_db_cve_by_ip,
)

app = FastAPI(title="CVE Manager")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialisation de la DB
init_db()

# Servir /static/ (CSS, JS…)
app.mount("/static", StaticFiles(directory="Web_app/static"), name="static")

# Templates Jinja2 (dossier templates/)
templates = Jinja2Templates(directory="Web_app/templates")

# ---- filtre Jinja pour pretty-print JSON ----
def json_pretty(value: Any) -> str:
    import json
    return json.dumps(value, ensure_ascii=False, indent=2)


templates.env.filters["json_pretty"] = json_pretty


# ===================== PAGES HTML ===================== #

@app.get("/", response_class=HTMLResponse)
def home(request: Request):
    """
    Page d’accueil simple : ton layout base.html
    """
    return templates.TemplateResponse(
        "home.html",
        {"request": request},
    )

@app.get("/failles", response_class=HTMLResponse)
def failles_page(
    request: Request,
    q: Optional[str] = Query(default=None),
    target: Optional[str] = Query(default=None),
    state: Optional[str] = Query(default=None),
    sort: str = Query(default="id_asc"),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=25, ge=1, le=200),
):
    """
    Page HTML qui liste les failles, avec filtres + tri + pagination.
    """
    if q is not None:
        q = q.strip() or None
    if target is not None:
        target = target.strip() or None
    if state is not None:
        state = state.strip() or None

    total = count_cves(q=q, target=target, state=state)
    rows = list_cves_paged(
        q=q,
        target=target,
        state=state,
        sort=sort,
        page=page,
        page_size=size,
    )
    total_pages = max(1, (total + size - 1) // size)

    context = {
        "request": request,
        "rows": rows,
        "total": total,
        "page": page,
        "size": size,
        "total_pages": total_pages,
        "q": q or "",
        "target": target or "",
        "state": state or "",
        "sort": sort,
    }
    return templates.TemplateResponse("failles.html", context)

@app.get("/states", response_class=HTMLResponse)
def states_page(
    request: Request,
    target: Optional[str] = Query(default="")
    ):
    """
    Page HTML qui affiche des states
    """
    if target:
        target = target.strip()
        if target not in ["name", "target", "state", "severity"]:
            target = "name"
    else:
        target = "name"
    data = count_db_cve_by_ip(target)
    labels = [data[i][target] for i in range(len(data))]
    counts = [data[i]['COUNT'] for i in range(len(data))]
    context = {
        "request": request,
        "labels": labels,
        "counts": counts,
        "target": target,
    }
    return templates.TemplateResponse("states.html", context)

# ===================== API JSON v1 ===================== #

@app.get("/v1/cves/")
def api_list_cves(
    q: Optional[str] = Query(None),
    target: Optional[str] = Query(None),
    state: Optional[str] = Query(None),
    limit: int = Query(200, le=1000),
) -> List[Dict[str, Any]]:
    """
    API JSON : liste des CVE (simple, sans pagination HTML).
    """
    return list_cves(q=q, target=target, state=state, limit=limit)


@app.get("/v1/cves/{cve_id}")
def api_get_cve(cve_id: int) -> Dict[str, Any]:
    """
    API JSON : détail d'une CVE par ID.
    """
    row = get_cve(cve_id)
    if not row:
        raise HTTPException(404, "CVE not found")
    return row


# Redirections facultatives vers la doc automatique FastAPI
@app.get("/v1")
@app.get("/v1/")
def api_v1_root():
    return RedirectResponse(url="/docs")

@app.get("/favicon.ico")
def favicon():
    return FileResponse("Web_app/static/favicon.ico")
