import sqlite3, json
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple

DB_PATH = Path(__file__).with_name("dev.db")

def _dict_factory(cursor, row):
    # 1. Obtenir la description des colonnes
    description = cursor.description
    # 2. Parcourir chaque colonne avec son index
    result_dict = {}
    for idx, col in enumerate(description):
        # 3. Récupérer le nom de la colonne
        column_name = col[0]
        # 4. Récupérer la valeur dans la ligne
        value = row[idx]
        # 5. Ajouter au dictionnaire
        result_dict[column_name] = value
    # 6. Résultat final
    return result_dict

def _get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = _dict_factory
    conn.execute("PRAGMA foreign_keys=ON;")    # Active la gestion de clé étrangère
    conn.execute("PRAGMA journal_mode=WAL;")   # Mode Write-Ahead Logging (Lecture/Ecriture simultané)
    return conn

def init_db() -> None:
    with _get_conn() as conn:
        conn.execute("""
        CREATE TABLE IF NOT EXISTS cve (
            id      INTEGER PRIMARY KEY AUTOINCREMENT,
            name    TEXT NOT NULL,
            target  TEXT NOT NULL,
            state   TEXT NOT NULL,
            infos   TEXT NOT NULL        -- JSON sérialisé
        );
        """)
        # seed si vide
        result = conn.execute("SELECT COUNT(*) AS number FROM cve;").fetchone()
        n = result["number"]
        if n == 0:
            rows = [
                ("CVE-2025-0001", "Android", "open", {"cvss": 7.8, "notes": "demo 1"}),
                ("CVE-2025-0002", "Linux",   "closed", {"cvss": 5.1, "notes": "demo 2"}),
                ("CVE-2025-0003", "WebApp",  "open", {"cvss": 9.0, "notes": "demo 3"}),
            ]
            conn.executemany(
                "INSERT INTO cve (name, target, state, infos) VALUES (?, ?, ?, ?);",
                [(a,b,c,json.dumps(d, ensure_ascii=False)) for (a,b,c,d) in rows]
            )
            # Insert chaque ligne de rows dans la base en transformant le dico en json non ASCII

def _decode_infos(row: Dict[str, Any]) -> Dict[str, Any]:
    if row is None:
        return row
    try:
        row["infos"] = json.loads(row["infos"]) if isinstance(row.get("infos"), str) else (row.get("infos") or {})
    except Exception:
        row["infos"] = {}
    return row

def _params_clauses(params, sql, q: Optional[str]=None, target: Optional[str]=None, state: Optional[str]=None):
    clauses = []
    if q:
        clauses.append("(name LIKE ? OR target LIKE ? OR state LIKE ?)")
        like = f"%{q}%"       # Pour les LIKE SQL %coucou%, coucou étant q
        params += [like, like, like]
    if target:
        clauses.append("target = ?"); params.append(target)
    if state:
        clauses.append("state = ?"); params.append(state)
    if clauses:
        sql += " WHERE " + " AND ".join(clauses)
    return params, sql

def list_cves(q: Optional[str]=None, target: Optional[str]=None, state: Optional[str]=None, limit: int=200) -> List[Dict[str, Any]]:
    sql = "SELECT id, name, target, state, infos FROM cve"
    params, sql = _params_clauses([], sql, q, target, state)
    sql += " ORDER BY id ASC LIMIT ?"
    params.append(min(limit, 1000))
    with _get_conn() as conn:
        rows = conn.execute(sql, params).fetchall()
    return [_decode_infos(r) for r in rows]

def get_cve(cve_id: int) -> Optional[Dict[str, Any]]:
    with _get_conn() as conn:
        row = conn.execute("SELECT id, name, target, state, infos FROM cve WHERE id=?", (cve_id,)).fetchone()
    return _decode_infos(row)


# mapping tri -> colonne SQL (pour éviter l'injection)
_SORT_MAP = {
    "id_asc":   "id ASC",
    "id_desc":  "id DESC",
    "name_asc": "name COLLATE NOCASE ASC",
    "name_desc":"name COLLATE NOCASE DESC",
    "target_asc":"target COLLATE NOCASE ASC",
    "target_desc":"target COLLATE NOCASE DESC",
}

def count_cves(q: Optional[str]=None, target: Optional[str]=None, state: Optional[str]=None) -> int:
    sql = "SELECT COUNT(*) AS number FROM cve"
    params, sql = _params_clauses([], sql, q, target, state)
    with _get_conn() as conn:
        result = conn.execute(sql, params).fetchone()
        return result["number"]

def list_cves_paged(q: Optional[str]=None, target: Optional[str]=None, state: Optional[str]=None, sort: str="id_asc", page: int=1, page_size: int=25) -> List[Dict[str, Any]]:
    sql = "SELECT id, name, target, state, infos FROM cve"
    params, sql = _params_clauses([], sql, q, target, state)
    order_by = _SORT_MAP.get(sort, "id ASC")
    sql += f" ORDER BY {order_by} LIMIT ? OFFSET ?"
    page = max(1, int(page))
    page_size = max(1, min(int(page_size), 200))
    offset = (page - 1) * page_size
    params += [page_size, offset]

    with _get_conn() as conn:
        rows = conn.execute(sql, params).fetchall()
    return [_decode_infos(r) for r in rows]