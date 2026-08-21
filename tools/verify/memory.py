"""Memory engine verification against a real SQLite engine using the app's
actual DDL and the actual DAO SQL strings."""
import sqlite3, re, pathlib
root = pathlib.Path('/home/user/Arena-spa/app/src/main/java/com/ronin/ai')
con = sqlite3.connect(':memory:')
con.execute("""CREATE TABLE memories (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, title TEXT,
              content TEXT, tags TEXT, source TEXT, createdAt INTEGER, updatedAt INTEGER, importance INTEGER)""")
def add(t,ti,c,imp=1,tags='',up=1000):
    con.execute("INSERT INTO memories (type,title,content,tags,source,createdAt,updatedAt,importance) VALUES (?,?,?,?,?,?,?,?)",
                (t,ti,c,tags,'user',1000,up,imp))
add('PREFERENCE','User name','Arjun',3)
add('PREFERENCE','User likes','black coffee',2)
add('LONG_TERM','Guitar','wants to learn guitar',2,'music')
add('CONVERSATION','chat','some chat text about coffee',1)
add('LEARNED_SOLUTION','Fix','restart resolves it',2)
con.commit()

# real DAO queries
searchNonConv = ("SELECT * FROM memories WHERE (title LIKE '%' || ? || '%' OR content LIKE '%' || ? || '%') "
                 "AND type != 'CONVERSATION' ORDER BY importance DESC, updatedAt DESC LIMIT ?")
countMatching = "SELECT COUNT(*) FROM memories WHERE type = ? AND title = ? AND content = ?"
observeSearch = ("SELECT * FROM memories WHERE title LIKE '%' || ? || '%' OR content LIKE '%' || ? || '%' "
                 "OR tags LIKE '%' || ? || '%' ORDER BY importance DESC, updatedAt DESC")

r=con.execute(searchNonConv,('coffee','coffee',6)).fetchall()
print("recall 'coffee' excludes CONVERSATION :", [x[1] for x in r] == ['PREFERENCE'], [x[2] for x in r])
print("dedup exact match found              :", con.execute(countMatching,('PREFERENCE','User name','Arjun')).fetchone()[0]==1)
print("dedup different content -> 0         :", con.execute(countMatching,('PREFERENCE','User name','Bob')).fetchone()[0]==0)
print("tag search hits                      :", len(con.execute(observeSearch,('music','music','music')).fetchall())==1)
# importance ordering
r=con.execute(searchNonConv,('','',10)).fetchall()
print("ordered by importance DESC           :", [x[8] for x in r]==sorted([x[8] for x in r],reverse=True))
# SQL-injection style input must be safely parameterised
evil = "'; DROP TABLE memories; --"
con.execute(searchNonConv,(evil,evil,5)).fetchall()
print("injection-safe (table intact)        :", con.execute("SELECT COUNT(*) FROM memories").fetchone()[0]==5)
# unicode
add('LONG_TERM','हिंदी','मुझे चाय पसंद है',2)
con.commit()
print("unicode recall                       :", len(con.execute(searchNonConv,('चाय','चाय',5)).fetchall())==1)
