import os
import re
from pathlib import Path
from typing import Optional


SECUENCIA_PREFIJO = "secuencia"      
SECUENCIA_EXTENSION = ".log"        


def getLastSequence(directory: Path) -> Optional[Path]:
 
    candidates = []

    for f in directory.iterdir():
        if f.is_file() and f.name.startswith(SECUENCIA_PREFIJO) and f.name.endswith(SECUENCIA_EXTENSION):
            candidates.append(f)

    if not candidates:
        return None

    # Ordenamos y nos quedamos con el más reciente
    latest = max(candidates, key=lambda p: p.stat().st_mtime)
    return latest


def readFile(path: Path) -> str: #lee el archivo como un string
 
    with path.open("r", encoding="utf-8") as f:
        return f.read()


def analyzewithRegex(sequence: str):
    """
    Usa re.subn con el patrón de T0..T16 y va limpiando la secuencia con los matchs, dejando los grupos
    que conservamos como reemplazo, para seguir limpiando, hasta que el archivo quede vacio, significando
    que se cumplieron todas las transiciones de forma correcta.
    """

    pattern = r"(T12)(.*?)(T0)(.*?)(T1(?!\d))(.*?)(((T2)(.*?)(T3)(.*?)(T4))|(T5)(.*?)(T6)|(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10))(.*?)(T11)"
    replacement = r"\g<2>\g<4>\g<6>\g<10>\g<12>\g<15>\g<18>\g<20>\g<22>\g<24>"

    remaining = sequence
    iteration = 0

    while True:
        iteration += 1
        cleaned, count = re.subn(pattern, replacement, remaining)

        if count == 0:
            break

        remaining = cleaned

    print(f"[Regex] Iteraciones realizadas: {iteration - 1}")

    if remaining.strip() != "":
        print("[Regex] Quedó contenido fuera de lugar después de aplicar todos los patrones:")
        print(remaining)
        return remaining

    print("[Regex] La secuencia quedó vacía. Todas las transiciones cumplen el patrón.")

def countInvariants(sequence: str):
    """
    Extrae invariantes T0..T16 uno por uno usando el patrón grande,
    y para cada invariante intenta clasificarlo contra la lista de
    posibles patrones (possiblesPatterns). Devuelve un dict con los
    conteos por tipo y la cantidad total.
    """

    possiblesPatterns = [
        (r'T12.*?T0.*?T1.*?T2.*?T3.*?T4.*?T11', 'T12-T0-T1-T2-T3-T4-T11'),
        (r'T12.*?T0.*?T1.*?T5.*?T6.*?T11', 'T12-T0-T1-T5-T6-T11'),
        (r'T12.*?T0.*?T1.*?T7.*?T8.*?T9.*?T10.*?T11', 'T12-T0-T1-T7-T8-T9-T10-T11'),
    ]

    pattern = re.compile(
    r"(T12)(.*?)(T0)(.*?)(T1(?!\d))(.*?)"
    r"(((T2)(.*?)(T3)(.*?)(T4))|(T5)(.*?)(T6)|(T7)(.*?)(T8)(.*?)(T9)(.*?)(T10))"
    r"(.*?)(T11)",
    re.S
    )


    replacement = r"\g<2>\g<4>\g<6>\g<10>\g<12>\g<15>\g<18>\g<20>\g<22>\g<24>"

    # Inicializo contador por tipo de invariante
    counts = {label: 0 for _, label in possiblesPatterns}
    total_invariants = 0

    remaining = sequence

    while True:
        m = pattern.search(remaining)
        if not m:
            break

        invariant_block = m.group(0)

        # Lo clasifico
        classified = False
        for regex, label in possiblesPatterns:
            if re.search(regex, invariant_block):
                counts[label] += 1
                classified = True
                break

        if not classified:
            print("Invariante no clasificado, bloque:")
            print(invariant_block)
            print("-" * 40)

        total_invariants += 1

        # Reconstruyo la secuencia "remaining" como hacía re.subn,

        before = remaining[:m.start()]
        after = remaining[m.end():]
        cleaned_middle = m.expand(replacement)
        remaining = before + cleaned_middle + after

    return counts, total_invariants, remaining


def main():
    # Directorio donde buscar (actual)
    current_dir = Path(__file__).parent

    latest_file = getLastSequence(current_dir)
    if latest_file is None:
        print("No se encontró ningún archivo que empiece con 'secuencia' y termine en '.log'.")
        return

    print(f"Archivo de secuencia más reciente: {latest_file.name}\n")

    


    # Extraemos la fecha del archivo para generar el log 
    # Formato esperado: secuencia_YYYY-MM-DD_HH-MM-SS.log
    file_name = latest_file.name
    date_part = file_name.replace("secuencia_", "").replace(".log", "")
    output_filename = f"invariantes_{date_part}.log"
    output_path = latest_file.parent / output_filename  


    sequence_content = readFile(latest_file)

    # Parte estructural
    regex_result = analyzewithRegex(sequence_content)

    counts, total, leftover = countInvariants(sequence_content)

    # Construimos el log
    log_text = []
    log_text.append(f"Archivo analizado: {latest_file.name}\n")
    log_text.append("[INVARIANTES POR TIPO]")

    for label, c in counts.items():
        log_text.append(f"{label}: {c}")

    log_text.append(f"\nTotal de invariantes detectados: {total}\n")

    if leftover.strip():
        log_text.append("Quedó contenido sin clasificar:")
        log_text.append(leftover)
    else:
        log_text.append("No quedó nada sin filtrar. Todos los invariantes fueron clasificados correctamente.")

    # guardamos el log
    with output_path.open("w", encoding="utf-8") as f:
        f.write("\n".join(log_text))

    print(f"\nLog generado correctamente en: {output_filename}\n")



if __name__ == "__main__":
    main()
