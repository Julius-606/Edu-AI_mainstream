import logging
import json
from typing import List, Optional
from sqlalchemy.orm import Session
from app.models import database_models as models

logger = logging.getLogger("curriculum_service")

# Curated unit learning outcomes for core academic & clinical units
DEFAULT_UNIT_LEARNING_OUTCOMES = {
    "Biochemistry II": [
        "Master the regulatory kinetics of glycolysis and the allosteric modulation of Phosphofructokinase-1 (PFK-1) by ATP, AMP, and Fructose 2,6-bisphosphate.",
        "Detail the enzymatic subunits (E1, E2, E3) and 5 essential coenzymes (TPP, Lipoate, CoA, FAD, NAD+) of the Pyruvate Dehydrogenase Complex (PDC).",
        "Describe the mitochondrial electron transport chain (ETC) complexes I-IV, electron carriers, and generation of the proton-motive force.",
        "Analyze the rotational catalysis mechanism of ATP synthase (Complex V) and the pathophysiology of ETC uncouplers (DNP, thermogenin) vs inhibitors (cyanide, CO, oligomycin)."
    ],
    "General Surgery": [
        "Perform structured clinical evaluation for acute appendicitis including McBurney's point tenderness, Rovsing's, Psoas, and Obturator signs.",
        "Interpret diagnostic laboratory markers (leukocytosis, elevated CRP) and imaging studies (graded compression ultrasound showing non-compressible appendix >6mm, CT scans).",
        "Formulate preoperative fluid resuscitation protocols, prophylactic antibiotic regimens, and surgical strategy (laparoscopic vs open appendectomy).",
        "Identify and manage early postoperative complications including surgical site infections, pelvic abscesses, stump leakages, postoperative ileus, and bowel obstruction."
    ],
    "Internal Medicine": [
        "Distinguish between Heart Failure with Reduced Ejection Fraction (HFrEF) and Heart Failure with Preserved Ejection Fraction (HFpEF) regarding ventricular remodeling, compliance, and hemodynamic pathophysiology.",
        "Formulate evidence-based guideline-directed medical therapy (GDMT) including ACE inhibitors/ARNIs, Beta-blockers, SGLT2 inhibitors, and Mineralocorticoid Receptor Antagonists.",
        "Interpret diagnostic findings on transthoracic echocardiography, chest radiography (Kerley B lines, cardiomegaly), and serum BNP / NT-proBNP assays.",
        "Classify acute decompensated heart failure patients using hemodynamic profiles (warm vs cold, wet vs dry) to direct intravenous loop diuretics, vasodilators, and inotropic support."
    ],
    "Cardiology": [
        "Master cardiac electrophysiology, action potential phases, and diagnostic identification of life-threatening cardiac arrhythmias.",
        "Differentiate ischemic heart diseases, acute coronary syndromes (STEMI vs NSTEMI/unstable angina), and biomarker troponin kinetics.",
        "Evaluate valvular heart diseases (aortic stenosis, mitral regurgitation) using auscultation findings and Doppler echocardiography.",
        "Apply guideline-directed acute and chronic management algorithms for congestive heart failure and cardiogenic shock."
    ],
    "Pathology": [
        "Explain cellular injury mechanisms, oxidative stress, apoptosis vs necrosis, and intracellular accumulations.",
        "Analyze acute and chronic inflammation pathways, chemical mediators, hemodynamic changes, and granulomatous tissue reaction.",
        "Differentiate benign vs malignant neoplasms, hallmarks of cancer, tumor staging/grading, and paraneoplastic syndromes.",
        "Interpret histopathological patterns and clinical lab panels in systemic autoimmune diseases and immunodeficiencies."
    ],
    "Pharmacology": [
        "Calculate pharmacokinetic parameters (clearance, volume of distribution, half-life, steady-state concentration) and receptor pharmacodynamics.",
        "Detail autonomic nervous system pharmacology, sympathomimetic, sympatholytic, parasympathomimetic, and anticholinergic agents.",
        "Formulate cardiovascular drug regimens: antihypertensives, antiarrhythmics, diuretics, and anticoagulant/antiplatelet mechanisms.",
        "Select targeted antimicrobial therapies based on mechanism of action, bacterial resistance patterns, and minimal inhibitory concentration (MIC)."
    ]
}

def get_unit_learning_outcomes(
    unit_name: str,
    db: Session,
    user: Optional[models.User] = None,
    subtopic_name: Optional[str] = None,
    client_outcomes: Optional[List[str]] = None
) -> List[str]:
    """
    Retrieves the authoritative learning outcomes for a specified unit.
    When a particular subtopic is selected, its direct learning objectives are prioritized
    at the top of the outcome list, followed by the unit's broader curriculum learning outcomes.
    """
    subtopic_outcomes: List[str] = []
    unit_outcomes: List[str] = []
    seen = set()

    clean_unit_name = unit_name.strip() if unit_name else ""
    clean_subtopic_name = subtopic_name.strip() if subtopic_name else ""

    # 1. Search for Unit in Database
    unit = None
    if user:
        unit = db.query(models.Unit).filter(
            models.Unit.owner_id == user.id,
            models.Unit.name.ilike(f"%{clean_unit_name}%")
        ).first()

    if not unit:
        unit = db.query(models.Unit).filter(
            models.Unit.name.ilike(f"%{clean_unit_name}%")
        ).first()

    # If unit not found by name, try locating via subtopic name
    if not unit and clean_subtopic_name:
        sub = db.query(models.Subtopic).filter(
            models.Subtopic.name.ilike(f"%{clean_subtopic_name}%")
        ).first()
        if sub and sub.topic and sub.topic.module and sub.topic.module.unit:
            unit = sub.topic.module.unit

    # 2. Extract objectives from database Unit hierarchy
    if unit:
        # Check explicit unit.learning_outcomes field if set
        if getattr(unit, "learning_outcomes", None):
            try:
                parsed = json.loads(unit.learning_outcomes)
                if isinstance(parsed, list):
                    for item in parsed:
                        s_item = str(item).strip()
                        if s_item and s_item not in seen:
                            seen.add(s_item)
                            unit_outcomes.append(s_item)
            except Exception:
                for line in unit.learning_outcomes.split("\n"):
                    s_line = line.strip(" -*•\t\r")
                    if s_line and s_line not in seen:
                        seen.add(s_line)
                        unit_outcomes.append(s_line)

        # Traverse modules -> topics -> subtopics -> learning_objectives
        for module in unit.modules:
            for topic in module.topics:
                for sub in topic.subtopics:
                    is_target_subtopic = False
                    if clean_subtopic_name:
                        s_name = sub.name.lower().strip()
                        c_name = clean_subtopic_name.lower().strip()
                        if s_name == c_name or c_name in s_name or s_name in c_name:
                            is_target_subtopic = True

                    for lo in sub.learning_objectives:
                        desc = lo.description.strip() if lo.description else ""
                        if desc and desc not in seen:
                            seen.add(desc)
                            if is_target_subtopic:
                                subtopic_outcomes.append(desc)
                            else:
                                unit_outcomes.append(desc)

    # 3. Check curated default unit outcomes if list is short or unit was not in DB
    for key, curated_list in DEFAULT_UNIT_LEARNING_OUTCOMES.items():
        if key.lower() in clean_unit_name.lower() or clean_unit_name.lower() in key.lower():
            for outcome in curated_list:
                desc = outcome.strip()
                if desc and desc not in seen:
                    seen.add(desc)
                    # Check if outcome is relevant to chosen subtopic
                    if clean_subtopic_name and clean_subtopic_name.lower() in desc.lower():
                        subtopic_outcomes.append(desc)
                    else:
                        unit_outcomes.append(desc)
            break

    # 4. Integrate client-supplied outcomes if any
    if client_outcomes:
        for c_out in client_outcomes:
            c_desc = str(c_out).strip()
            if c_desc and c_desc not in seen:
                seen.add(c_desc)
                subtopic_outcomes.append(c_desc)

    # Combine: chosen subtopic outcomes FIRST, followed by unit-wide outcomes
    combined_outcomes = subtopic_outcomes + unit_outcomes

    # 5. Fallback synthesis if no outcomes were found in DB or presets
    if not combined_outcomes:
        if clean_subtopic_name:
            combined_outcomes = [
                f"Master the pathophysiological mechanisms, clinical evaluation, and diagnostic criteria of {clean_subtopic_name} within {clean_unit_name}.",
                f"Formulate evidence-based clinical management and targeted therapeutic interventions for conditions arising under {clean_subtopic_name}.",
                f"Identify critical diagnostic traps, laboratory markers, and differential considerations relevant to {clean_subtopic_name}."
            ]
        else:
            combined_outcomes = [
                f"Synthesize foundational pathophysiological and clinical principles governing {clean_unit_name}.",
                f"Formulate evidence-based therapeutic strategies, diagnostic interpretation, and patient stabilization in {clean_unit_name}.",
                f"Demonstrate high-yield active recall and differential diagnostic accuracy across all modules in {clean_unit_name}."
            ]

    logger.info(f"Retrieved {len(combined_outcomes)} learning outcomes for unit '{unit_name}' (subtopic: '{subtopic_name}')")
    return combined_outcomes
