#!/usr/bin/env python3
"""Verify every `viewModel.x` / `viewModel::x` reference in a Compose screen
resolves to a real member of the matching ViewModel (incl. inherited ViewModel
members). Catches the classic "renamed a VM property, screen still calls the
old name" breakage that only shows up at compile time."""
import re, pathlib, sys

root = pathlib.Path('app/src/main/java/com/ronin/ai')
vms = {p.stem: p for p in root.rglob('*ViewModel.kt')}
base = {'viewModelScope', 'onCleared', 'addCloseable', 'getCloseable', 'equals',
        'hashCode', 'toString'}

member_re = re.compile(
    r'^\s*(?:@\w+\s+)*(?:public |internal |private |protected |open |override |abstract |final |lateinit |const )*'
    r'(?:val|var|fun)\s+(?:<[^>]*>\s+)?([A-Za-z_]\w*)', re.M)

problems = []
for screen in sorted(root.rglob('*Screen.kt')) + sorted(root.rglob('*Dialog.kt')):
    text = screen.read_text()
    m = re.search(r'viewModel:\s*(\w+ViewModel)', text)
    if not m:
        continue
    vm_name = m.group(1)
    if vm_name not in vms:
        problems.append((screen, vm_name, '<ViewModel file not found>'))
        continue
    vm_text = vms[vm_name].read_text()
    members = set(member_re.findall(vm_text)) | base
    # enum/const style members and constructor params
    members |= set(re.findall(r'^\s*(?:private\s+)?(?:val|var)\s+(\w+)', vm_text, re.M))
    used = set(re.findall(r'viewModel(?:\.|::)(\w+)', text))
    for u in sorted(used - members):
        problems.append((screen, vm_name, u))

print(f"screens checked : {len(list(root.rglob('*Screen.kt')))}")
print(f"viewmodels      : {len(vms)}")
print(f"bad references  : {len(problems)}")
for s, vm, u in problems:
    print(f"  {s.relative_to(root)}: {vm}.{u}")
sys.exit(1 if problems else 0)
