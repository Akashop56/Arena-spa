"""Demonstrate the CURRENT recall behaviour: keywords() keeps stopwords,
take(3) then picks the first three, so real query terms get crowded out."""
import re, sqlite3
def keywords(s):
    return [w for w in re.split(r'[^a-z0-9\u0900-\u097F]+', s.lower()) if len(w)>=2]

con=sqlite3.connect(':memory:')
con.execute("""CREATE TABLE memories (id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,title TEXT,
              content TEXT,tags TEXT,source TEXT,createdAt INTEGER,updatedAt INTEGER,importance INTEGER)""")
rows=[('PREFERENCE','User likes','I love black coffee in the morning',2),
      ('PREFERENCE','User name','Arjun',3),
      ('LONG_TERM','Guitar goal','wants to learn guitar this year',2),
      ('LONG_TERM','Work','works at a hospital in Kanpur',2),
      ('LONG_TERM','Allergy','allergic to peanuts',3)]
for i,(t,ti,c,imp) in enumerate(rows):
    con.execute("INSERT INTO memories (type,title,content,tags,source,createdAt,updatedAt,importance) VALUES (?,?,?,?,?,?,?,?)",
                (t,ti,c,'','user',1000,1000+i,imp))
con.commit()
Q="SELECT title,content FROM memories WHERE (title LIKE '%'||?||'%' OR content LIKE '%'||?||'%') AND type!='CONVERSATION' ORDER BY importance DESC, updatedAt DESC LIMIT 6"

for q in ["what do you know about my coffee preference",
          "am I allergic to anything?",
          "do you remember what instrument I want to learn"]:
    terms=keywords(q)[:3]
    hits={}
    for t in terms:
        for r in con.execute(Q,(t,t)).fetchall(): hits[r]=1
    print(f"\nquery : {q}")
    print(f"terms used (first 3) : {terms}")
    print(f"recalled: {[h[0] for h in hits] or 'NOTHING'}")
