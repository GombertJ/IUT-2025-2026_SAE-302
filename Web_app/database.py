import sqlite3, json
from pathlib import Path
from typing import List, Dict, Any, Optional
from collections import defaultdict
from contextlib import contextmanager

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

def _close_conn(conn: sqlite3.Connection):
    if conn:
        conn.close()

@contextmanager
def get_connection():
    conn = _get_conn()
    try:
        yield conn
    finally:
        _close_conn(conn)

from faker import Faker
import random
import json

fake = Faker()

def generate_row():
    cve_id = f"CVE-{fake.year()}-{random.randint(1000,9999):04d}"
    product = fake.word().capitalize()
    status = random.choice(["open", "closed"])
    severity = random.choice(["critical", "high", "medium", "low"])
    details = {
        "template-id": fake.sentence(),
        "matched-at": fake.sentence(),
        "info": fake.sentence(),
        "name": fake.sentence(),
        "severity": severity,
    }
    return (cve_id, product, status, details)

def init_db() -> None:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = _dict_factory
    conn.execute("""
    CREATE TABLE IF NOT EXISTS cve (
        id      INTEGER PRIMARY KEY AUTOINCREMENT,
        name    TEXT NOT NULL,
        target  TEXT NOT NULL,
        state   TEXT NOT NULL,
        infos   TEXT NOT NULL
    );
    """)
    result = conn.execute("SELECT COUNT(*) AS number FROM cve;").fetchone()
    n = result["number"]
    if n == 0:
        rows = [generate_row() for _ in range(1000)]
        for (a,b,c,d) in rows:
            conn.execute("INSERT INTO cve (name, target, state, infos) VALUES (?, ?, ?, ?);",
                        (a, b, c, json.dumps(d, ensure_ascii=False)))
    conn.commit()
    conn.close()
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
    with get_connection() as conn:
        rows = conn.execute(sql, params).fetchall()
        _close_conn(conn)
    return [_decode_infos(r) for r in rows]

def get_cve(cve_id: int) -> Optional[Dict[str, Any]]:
    with get_connection() as conn:
        row = conn.execute("SELECT id, name, target, state, infos FROM cve WHERE id=?", (cve_id,)).fetchone()
        _close_conn(conn)
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
    with get_connection() as conn:
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

    with get_connection() as conn:
        rows = conn.execute(sql, params).fetchall()
    return [_decode_infos(r) for r in rows]

def count_db_cve_by_ip(target):
    with get_connection() as conn:
        if target == "severity":
            rows = conn.execute("SELECT infos FROM cve").fetchall()

            severity_counts = defaultdict(int)

            for row in rows:
                infos_json = row['infos']
                try:
                    infos = json.loads(infos_json)

                    info_raw = infos.get("info")
                    if not info_raw:
                        continue

                    info_decoded = urllib.parse.unquote(info_raw)
                    info = json.loads(info_decoded)

                    severity_value = info.get("severity")
                    if severity_value is not None:
                        severity_counts[severity_value] += 1

                except json.JSONDecodeError:
                    continue

            data = [
                {"severity": severity, "COUNT": count}
                for severity, count in severity_counts.items()
            ]
            data = sorted(data, key=lambda x: str(x["severity"]))
        else:
            query = f"SELECT {target}, COUNT(*) AS COUNT FROM cve GROUP BY {target}"
            data = conn.execute(query).fetchall()
    return data
