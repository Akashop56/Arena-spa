#!/usr/bin/env python3
"""Validate every Room @Query against a real SQLite engine.

Room's annotation processor compiles and verifies each @Query at build time.
KSP can't run here, so this rebuilds the schema from the @Entity classes and
runs every query through sqlite3 to catch bad SQL / wrong column names.
"""
import re, pathlib, sqlite3, sys

root = pathlib.Path('app/src/main/java/com/ronin/ai')

KT2SQL = {'Long':'INTEGER','Int':'INTEGER','Boolean':'INTEGER','Float':'REAL',
          'Double':'REAL','String':'TEXT'}

# ---- build schema from entities ----
con = sqlite3.connect(':memory:')
tables = {}
for f in root.rglob('*Entity.kt'):
    t = f.read_text()
    m = re.search(r'@Entity\s*\(([^)]*)\)', t, re.S)
    if not m: continue
    tn = re.search(r'tableName\s*=\s*"(\w+)"', m.group(1))
    if not tn: continue
    table = tn.group(1)
    cls = re.search(r'data class (\w+)\s*\((.*?)\n\)', t, re.S)
    if not cls: continue
    cols = []
    for line in cls.group(2).split('\n'):
        line = line.strip().rstrip(',')
        cm = re.search(r'(?:@PrimaryKey[^)]*\)\s*)?val\s+(\w+)\s*:\s*(\w+)(\?)?', line)
        if not cm: continue
        name, ktype = cm.group(1), cm.group(2)
        cols.append(f'"{name}" {KT2SQL.get(ktype,"TEXT")}')
    if cols:
        ddl = f'CREATE TABLE {table} ({", ".join(cols)})'
        con.execute(ddl)
        tables[table] = ddl

print(f"tables built: {len(tables)} -> {', '.join(sorted(tables))}")

# ---- extract and test every @Query ----
bad, total = [], 0
qre = re.compile(r'@Query\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)\)\s*(?:suspend\s+)?fun\s+(\w+)', re.S)
for f in sorted(root.rglob('*Dao.kt')):
    t = f.read_text()
    for m in qre.finditer(t):
        raw, fn = m.group(1), m.group(2)
        sql = ''.join(re.findall(r'"((?:[^"\\]|\\.)*)"', raw)).replace('\\"','"')
        # Room binds :params -> use SQLite named params
        sql_test = re.sub(r':(\w+)', r':\1', sql)
        params = {p: 1 for p in re.findall(r':(\w+)', sql)}
        total += 1
        try:
            con.execute(sql_test, params)
        except Exception as e:
            bad.append((f.name, fn, sql[:110], str(e)))

print(f"queries tested: {total}")
print(f"FAILURES      : {len(bad)}")
for fn_file, fn, sql, err in bad:
    print(f"  {fn_file}::{fn}\n     {sql}\n     -> {err}")
sys.exit(1 if bad else 0)
