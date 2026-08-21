#!/usr/bin/env python3
"""Verify the Hilt graph: every constructor-injected dependency is satisfiable.

Hilt fails the build with "cannot be provided without an @Provides-annotated
method". KSP can't run here, so we resolve each @Inject constructor parameter
against @Provides, @Binds, other @Inject constructors, and known Hilt built-ins.
"""
import re, pathlib, sys, collections

root = pathlib.Path('app/src/main/java/com/ronin/ai')
src = {f: f.read_text() for f in sorted(root.rglob('*.kt'))}

provided = set()
binds_impl = {}
inject_ctors = {}       # type -> [param types]
declared_types = set()

BUILTIN = {'Context','Application','CoroutineScope','SavedStateHandle',
           'Gson','OkHttpClient','Retrofit','Set','Map'}

for f, t in src.items():
    for c in re.findall(r'^\s*(?:@\w+\s+)*(?:abstract |open |sealed |data )*class\s+(\w+)', t, re.M):
        declared_types.add(c)
    for i in re.findall(r'^\s*interface\s+(\w+)', t, re.M):
        declared_types.add(i)
    for o in re.findall(r'^\s*object\s+(\w+)', t, re.M):
        declared_types.add(o)

    # @Provides fun x(...): ReturnType
    for m in re.finditer(r'@Provides[\s\S]{0,120}?fun\s+\w+\s*\([^)]*\)\s*:\s*([\w<>., ?]+)', t):
        provided.add(m.group(1).split('<')[0].strip())
    # @Binds abstract fun x(impl: Impl): Iface
    for m in re.finditer(r'@Binds[\s\S]{0,140}?fun\s+\w+\s*\(\s*\w+\s*:\s*(\w+)\s*\)\s*:\s*(\w+)', t):
        binds_impl[m.group(2)] = m.group(1)
        provided.add(m.group(2))

    # @Inject constructor(...)
    for m in re.finditer(r'class\s+(\w+)[\s\S]{0,200}?@Inject\s+constructor\s*\(([\s\S]*?)\)\s*(?::|\{)', t):
        cls, body = m.group(1), m.group(2)
        params = []
        for line in body.split('\n'):
            pm = re.search(r'(?:private\s+|@\w+\s+)*val\s+\w+\s*:\s*([\w.]+)', line)
            if pm:
                params.append(pm.group(1).split('.')[-1])
        inject_ctors[cls] = params

satisfiable = set(provided) | set(inject_ctors) | BUILTIN | {v for v in binds_impl.values()}

problems = []
for cls, params in sorted(inject_ctors.items()):
    for p in params:
        if p in satisfiable or p in BUILTIN:
            continue
        problems.append((cls, p))

print(f"@Inject constructors : {len(inject_ctors)}")
print(f"@Provides types      : {len(provided)}")
print(f"@Binds pairs         : {len(binds_impl)}")
print(f"UNSATISFIED deps     : {len(problems)}")
for cls, p in problems:
    print(f"  {cls} needs {p}")
sys.exit(1 if problems else 0)
