import { Unit } from '../types';

export const DEFAULT_UNITS: Unit[] = [
  {
    id: 1,
    unitName: "Biochemistry II",
    category: "Pre-Clinical Sciences",
    description: "Advanced metabolic pathways, bioenergetics, enzymology kinetics, and clinical metabolic derangements.",
    isActive: true,
    modules: [
      {
        id: 101,
        unitId: 1,
        name: "Bioenergetics & Cellular Respiration",
        description: "Energy transformation, Krebs cycle intermediates, and mitochondrial electron transport.",
        topics: [
          {
            id: 1001,
            moduleId: 101,
            name: "Glycolysis & Pyruvate Fate",
            description: "Aerobic vs anaerobic glycolysis and the pyruvate dehydrogenase complex.",
            subtopics: [
              {
                id: 10001,
                topicId: 1001,
                name: "Regulation of Phosphofructokinase-1 (PFK-1)",
                isCompleted: true,
                objectives: [
                  {
                    id: "obj-1-1",
                    title: "Rate-Limiting Step of Glycolysis",
                    description: "Understand how PFK-1 is allosterically regulated by ATP, AMP, and Fructose 2,6-bisphosphate.",
                    content: `### Regulation of PFK-1 in Glycolytic Control

Phosphofructokinase-1 (PFK-1) catalyzes the committed rate-limiting step of glycolysis: converting **Fructose 6-phosphate** to **Fructose 1,6-bisphosphate** via ATP hydrolysis.

#### Key Allosteric Regulators:
- **Fructose 2,6-bisphosphate (F2,6-BP)**: The most potent positive allosteric activator. Produced by PFK-2 under insulin stimulation in the fed state. Overrides high-ATP inhibition.
- **AMP**: Signal of cellular energy depletion; strongly activates PFK-1.
- **ATP**: Negative allosteric inhibitor binding to low-affinity regulatory sites when energy charge is high.
- **Citrate**: High intracellular citrate (from active TCA cycle) signals abundant biosynthetic precursors and inhibits PFK-1.
- **Intracellular H+ (Acidosis)**: Inhibits PFK-1 to protect heart and brain tissues from excessive lactic acid accumulation during anaerobic distress.

> **Clinical Correlation**: In diabetic ketoacidosis (DKA) or starvation, high glucagon increases PKA activity, phosphorylating PFK-2/FBPase-2. This decreases F2,6-BP, slowing glycolysis and diverting substrates into gluconeogenesis.`
                  },
                  {
                    id: "obj-1-2",
                    title: "Pyruvate Dehydrogenase Complex (PDC) Integration",
                    description: "Trace pyruvate transport across the inner mitochondrial membrane into acetyl-CoA.",
                    content: `### The Pyruvate Dehydrogenase (PDH) Complex

The PDH complex links cytosolic glycolysis to the mitochondrial Krebs cycle. It consists of three enzymatic subunits requiring five coenzymes:
1. **E1 (Pyruvate Decarboxylase)**: Requires Thiamine Pyrophosphate (TPP / Vitamin B1).
2. **E2 (Dihydrolipoyl Transacetylase)**: Requires Lipoic acid & Coenzyme A (CoA-SH from Pantothenate).
3. **E3 (Dihydrolipoyl Dehydrogenase)**: Requires FAD (Riboflavin) and NAD+ (Niacin).

*Mnemonic: Tender Loving Care For Nancy (TPP, Lipoate, CoA, FAD, NAD).*

#### Clinical Note:
Arsenic poisoning inhibits lipoic acid, presenting with vomiting, rice-water stools, garlic-scented breath, and lactic acidosis.`
                  }
                ]
              },
              {
                id: 10002,
                topicId: 1001,
                name: "Electron Transport Chain & ATP Synthase",
                isCompleted: true,
                objectives: [
                  {
                    id: "obj-1-3",
                    title: "Proton Gradient & Chemiosmosis",
                    description: "Analyze proton pumping across Complexes I, III, and IV generating the proton motive force.",
                    content: `### The Chemiosmotic Hypothesis (Mitchell)

Electrons donated by **NADH** (Complex I) and **FADH2** (Complex II) flow downhill thermodynamically through Ubiquinone (CoQ) and Cytochrome c to oxygen (Complex IV, reducing O2 to H2O).

- **Complex I (NADH-Q oxidoreductase)**: Pumps 4 H+ per 2e-. Inhibited by Rotenone.
- **Complex III (Cytochrome bc1)**: Pumps 4 H+ per 2e-. Inhibited by Antimycin A.
- **Complex IV (Cytochrome c oxidase)**: Pumps 2 H+ per 2e-. Poisoned by Cyanide (CN-) and Carbon Monoxide (CO).
- **Complex V (F0F1 ATP Synthase)**: Uses proton backflow through F0 rotor to synthesize ATP. Inhibited by Oligomycin.

> **Uncouplers (e.g. 2,4-DNP, Aspirin overdose, Thermogenin in brown fat)** increase membrane proton permeability without ATP synthesis, dissipating energy as heat and causing hyperthermia.`
                  }
                ]
              }
            ]
          },
          {
            id: 1002,
            moduleId: 101,
            name: "Enzymology & Kinetic Models",
            description: "Michaelis-Menten dynamics, Lineweaver-Burk plots, and competitive vs noncompetitive inhibition.",
            subtopics: [
              {
                id: 10003,
                topicId: 1002,
                name: "Competitive vs Noncompetitive Inhibition",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-1-4",
                    title: "Lineweaver-Burk Plot Transformations",
                    description: "Differentiate effects on Km and Vmax for competitive, noncompetitive, and uncompetitive inhibitors.",
                    content: `### Kinetic Transformations & Enzyme Inhibition

On a Lineweaver-Burk double reciprocal plot:
- **Y-intercept** = 1 / Vmax
- **X-intercept** = -1 / Km
- **Slope** = Km / Vmax

#### Inhibition Types:
1. **Competitive Inhibitor** (e.g., Statins inhibiting HMG-CoA reductase, Methotrexate inhibiting DHFR):
   - Resembles substrate; binds active site.
   - Overcome by adding excess substrate.
   - **Km increases** (shifts right towards zero on X-axis).
   - **Vmax unchanged** (same Y-intercept).
2. **Noncompetitive Inhibitor** (Allosteric):
   - Binds allosteric site on enzyme or ES complex with equal affinity.
   - Cannot be overcome by high substrate.
   - **Km unchanged** (same X-intercept).
   - **Vmax decreases** (Y-intercept moves higher up).
3. **Uncompetitive Inhibitor**:
   - Binds *only* to ES complex.
   - Both **Km and Vmax decrease** proportionally (parallel lines).`
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        id: 102,
        unitId: 1,
        name: "Lipid Biochemistry & Dyslipidemias",
        description: "Fatty acid oxidation, ketone body utilization, and plasma lipoprotein cascades.",
        topics: [
          {
            id: 1003,
            moduleId: 102,
            name: "Beta-Oxidation & Ketogenesis",
            description: "Carnitine shuttle mechanism and acetoacetate production.",
            subtopics: [
              {
                id: 10004,
                topicId: 1003,
                name: "Carnitine Palmitoyltransferase (CPT-1) Shuttle",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-1-5",
                    title: "Fatty Acid Transport into Mitochondria",
                    description: "Learn how Malonyl-CoA inhibits CPT-1 to prevent futile fatty acid breakdown during synthesis.",
                    content: `### The Carnitine Shuttle

Long-chain fatty acids (>14 carbons) cannot freely penetrate the inner mitochondrial membrane.

1. **Fatty Acyl-CoA synthetase** activates fatty acids in the cytosol.
2. **CPT-1 (Carnitine Palmitoyltransferase I)** attaches carnitine to form acylcarnitine on outer mitochondrial membrane.
   - **Inhibited by Malonyl-CoA**: Ensures fatty acid synthesis and beta-oxidation do not occur simultaneously.
3. Translocase ferries acylcarnitine across inner membrane.
4. **CPT-2** restores Acyl-CoA inside mitochondrial matrix for beta-oxidation.

> **Deficiency in Carnitine Shuttle**: Causes hypoketotic hypoglycemia during fasting, muscle weakness, cardiomyopathy, and elevated serum free fatty acids.`
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 2,
    unitName: "General Surgery",
    category: "Clinical Sciences",
    description: "Surgical principles, trauma assessment, acute abdomen differentials, wound healing, and perioperative medicine.",
    isActive: true,
    modules: [
      {
        id: 201,
        unitId: 2,
        name: "Acute Abdomen & Surgical Emergencies",
        description: "Systematic physical exam, radiographic decision rules, and operative emergencies.",
        topics: [
          {
            id: 2001,
            moduleId: 201,
            name: "Appendicitis, Peritonitis & Perforation",
            description: "Clinical signs (Rovsing, Psoas, Obturator) and radiological diagnostic algorithms.",
            subtopics: [
              {
                id: 20001,
                topicId: 2001,
                name: "Acute Appendicitis Presentation & Alvarado Score",
                isCompleted: true,
                objectives: [
                  {
                    id: "obj-2-1",
                    title: "Pathogenesis & Clinical Diagnosis",
                    description: "Identify luminal obstruction by fecalith or lymphoid hyperplasia progressing to ischemia.",
                    content: `### Acute Appendicitis Clinical Roadmap

Obstruction of the appendiceal lumen by a **fecalith** (adults) or **lymphoid hyperplasia** (children, post-viral) produces intraluminal distension, bacterial proliferation, venous thrombosis, and transmural gangrene.

#### Classical Clinical Sequence:
1. **Dull periumbilical visceral pain** (referred along T10 dermatome via visceral afferents).
2. Anorexia ("hamburger sign" - loss of appetite strongly correlates).
3. **Nausea & vomiting**.
4. **Somatic localized sharp pain** shifting to McBurney's point (1/3 distance from ASIS to umbilicus) due to parietal peritoneum irritation.

#### Diagnostic Physical Maneuvers:
- **Rovsing Sign**: Palpation of LLQ elicits RLQ pain due to peritoneal reflection.
- **Psoas Sign**: Retrocecal appendix irritation upon passive hip hyperextension.
- **Obturator Sign**: Pelvic appendix irritation upon internal rotation of flexed right hip.`
                  }
                ]
              },
              {
                id: 20002,
                topicId: 2001,
                name: "Small Bowel Obstruction vs Paralytic Ileus",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-2-2",
                    title: "Air-Fluid Levels & Strangulation Signs",
                    description: "Differentiate mechanical adhesive obstruction from postoperative hypomotility.",
                    content: `### Small Bowel Obstruction (SBO) vs. Ileus

The leading cause of SBO in patients with previous abdominal surgery is **postoperative peritoneal adhesions** (60-70%), followed by incarcerated hernias and neoplasms.

#### Cardinal Symptoms of SBO:
- Colicky abdominal pain
- Bilious or feculent vomiting (earlier in proximal, later in distal)
- Obstipation (absence of both flatus and stool)
- High-pitched "tinkling" or hyperactive bowel sounds early, progressing to silent abdomen.

#### Plain Radiograph & CT Findings:
- Multiple dilated small bowel loops (>3 cm diameter) in central abdomen.
- Air-fluid levels arranged in a "stepladder" pattern.
- Absence of colonic gas (complete obstruction).

> **Signs of Closed-Loop Obstruction or Strangulation (Emergent OR)**: Fever, localized peritoneal guarding, tachycardia, leukocytosis with bandemia, continuous severe pain, or elevated serum lactate.`
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        id: 202,
        unitId: 2,
        name: "Trauma & Critical Surgical Resuscitation",
        description: "ATLS protocol, hemorrhagic shock staging, and massive transfusion guidelines.",
        topics: [
          {
            id: 2002,
            moduleId: 202,
            name: "Advanced Trauma Life Support (ATLS)",
            description: "Airway with cervical spine immobilization through secondary survey.",
            subtopics: [
              {
                id: 20003,
                topicId: 2002,
                name: "Hemorrhagic Shock Classification & MTP",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-2-3",
                    title: "ATLS Classes I-IV Hemorrhagic Shock",
                    description: "Memorize blood loss thresholds, blood pressure triggers, and 1:1:1 balanced resuscitation.",
                    content: `### Classification of Hemorrhagic Shock (ATLS 10th Ed)

- **Class I (<15% blood loss, <750 mL)**: Normal HR, normal BP, normal pulse pressure, minimal anxiety.
- **Class II (15-30% blood loss, 750-1500 mL)**: HR >100 bpm, normal BP, narrowed pulse pressure, mild tachypnea.
- **Class III (30-40% blood loss, 1500-2000 mL)**: HR >120 bpm, **Hypotension begins**, marked tachypnea, oliguria, confused.
- **Class IV (>40% blood loss, >2000 mL)**: HR >140 bpm, severe hypotension, negligible urine, lethargic/unconscious.

#### Balanced Hemostatic Resuscitation (MTP):
Avoid large volume crystalloids (which worsen hypothermia, coagulopathy, and acidosis — the lethal triad). Instead, trigger **Massive Transfusion Protocol (MTP)** using 1:1:1 ratio:
- 1 Unit Packed Red Blood Cells (PRBCs)
- 1 Unit Fresh Frozen Plasma (FFP)
- 1 Unit Platelets`
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 3,
    unitName: "Internal Medicine",
    category: "Clinical Sciences",
    description: "Pathophysiology, diagnostics, and pharmacotherapy across cardiovascular, respiratory, renal, and endocrine systems.",
    isActive: true,
    modules: [
      {
        id: 301,
        unitId: 3,
        name: "Cardiology & Vascular Dynamics",
        description: "Coronary artery disease, acute heart failure syndromes, arrhythmias, and ECG analysis.",
        topics: [
          {
            id: 3001,
            moduleId: 301,
            name: "Acute Coronary Syndromes (ACS)",
            description: "STEMI, NSTEMI, and unstable angina risk stratification and immediate intervention.",
            subtopics: [
              {
                id: 30001,
                topicId: 3001,
                name: "ECG Localization & Biomarker Dynamics",
                isCompleted: true,
                objectives: [
                  {
                    id: "obj-3-1",
                    title: "Coronary Arterial Anatomy & ECG Leads",
                    description: "Map ST elevations in reciprocal leads to LAD, RCA, and LCx occlusions.",
                    content: `### ECG Localization of Myocardial Infarction

Coronary arterial occlusion produces characteristic regional ST-segment elevations:

- **Anteroseptal (Leads V1-V2)**: Left Anterior Descending (LAD) artery.
- **Anterior (Leads V3-V4)**: LAD diagonal branches.
- **Anterolateral / Extensive Anterior (Leads V1-V6, I, aVL)**: Proximal LAD main stem.
- **Lateral (Leads I, aVL, V5, V6)**: Left Circumflex (LCx) or diagonal LAD branch.
- **Inferior (Leads II, III, aVF)**: Right Coronary Artery (RCA, in 85-90% right-dominant hearts) or LCx.
  - *Warning*: Inferior MI requires right-sided ECG (V4R) to check for Right Ventricular Infarction. In RV infarction, **nitrates are contraindicated** due to preload dependence!
- **Posterior (Reciprocal ST depression in V1-V3, tall broad R waves)**: RCA or LCx posterior descending artery.

#### Cardiac Troponin Biomarkers:
- **High-Sensitivity Troponin I / T**: Rises in 2-4 hours, peaks at 12-24 hours, persists for 7-14 days.
- **CK-MB**: Normalizes in 48-72 hours, ideal for diagnosing reinfarction.`
                  }
                ]
              },
              {
                id: 30002,
                topicId: 3001,
                name: "Heart Failure with Reduced vs Preserved Ejection Fraction",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-3-2",
                    title: "Guideline-Directed Medical Therapy (GDMT)",
                    description: "Learn the four foundational pillars of GDMT for HFrEF (EF ≤40%).",
                    content: `### Four Pillars of HFrEF Medical Therapy

Mortality in Heart Failure with Reduced Ejection Fraction (HFrEF) is reduced by four core drug classes:
1. **ARNI (Sacubitril-Valsartan)**: Inhibits neprilysin (preventing BNP breakdown) + blocks AT1 receptors.
2. **Beta-Blockers (Bisoprolol, Carvedilol, or Metoprolol Succinate)**: Counteracts chronic sympathetic overdrive.
3. **MRA (Spironolactone or Eplerenone)**: Mineralocorticoid receptor antagonists prevent myocardial fibrosis.
4. **SGLT2 Inhibitors (Dapagliflozin or Empagliflozin)**: Reduces cardiovascular death and hospitalization irrespective of diabetic status.`
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        id: 302,
        unitId: 3,
        name: "Nephrology & Acid-Base Balance",
        description: "Renal clearance, electrolyte imbalances, and acid-base blood gas interpretation.",
        topics: [
          {
            id: 3002,
            moduleId: 302,
            name: "Acid-Base Disturbances & Anion Gap",
            description: "High anion gap metabolic acidosis (MUDPILES) and compensatory mechanics.",
            subtopics: [
              {
                id: 30003,
                topicId: 3002,
                name: "Serum Anion Gap & Osmolar Gap",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-3-3",
                    title: "Systematic ABG Interpretation",
                    description: "Calculate Serum Anion Gap = Na - (Cl + HCO3). Normal range 8-12 mEq/L.",
                    content: `### High Anion Gap Metabolic Acidosis (HAGMA)

#### Anion Gap Calculation:
$$\\text{AG} = [\\text{Na}^+] - ([\\text{Cl}^-] + [\\text{HCO}_3^-])$$
Normal range: **8 to 12 mEq/L**.

#### Causes (Mnemonic: GOLD MARK or MUDPILES):
- **M** - Methanol
- **U** - Uremia (advanced renal failure)
- **D** - Diabetic Ketoacidosis (or alcoholic/starvation ketoacidosis)
- **P** - Propylene glycol
- **I** - Isoniazid / Iron overdose
- **L** - Lactic Acidosis (sepsis, tissue hypoperfusion, metformin)
- **E** - Ethylene Glycol (antifreeze; shows calcium oxalate crystals in urine)
- **S** - Salicylates (Aspirin; respiratory alkalosis + metabolic acidosis)`
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 4,
    unitName: "Clinical Pharmacology",
    category: "Therapeutics",
    description: "Drug receptor interactions, therapeutic index, neuropharmacology, and antimicrobial stewardship.",
    isActive: false,
    modules: [
      {
        id: 401,
        unitId: 4,
        name: "Antimicrobial Stewardship",
        description: "Mechanism of action and resistance patterns of antibacterials and antifungals.",
        topics: [
          {
            id: 4001,
            moduleId: 401,
            name: "Cell Wall & Protein Synthesis Inhibitors",
            description: "Penicillins, Cephalosporins, Aminoglycosides, and Macrolides.",
            subtopics: [
              {
                id: 40001,
                topicId: 4001,
                name: "Mechanism of Beta-Lactams & Beta-Lactamase Inhibitors",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-4-1",
                    title: "Penicillin-Binding Proteins (PBPs)",
                    description: "Understand cross-linking of peptidoglycan chains and clavulanic acid synergy.",
                    content: `### Beta-Lactam Antibiotic Mechanics

Beta-lactam antibiotics contain a four-membered lactam ring resembling the D-Ala-D-Ala terminus of peptidoglycan precursors. They bind irreversibly to transpeptidases (PBPs), inhibiting bacterial cell wall synthesis.

#### Overcoming Resistance:
Bacteria produce beta-lactamases that hydrolyze the ring. Co-formulations with suicide inhibitors (Clavulanic acid, Sulbactam, Tazobactam) restore activity against beta-lactamase-producing Staph, H. influenzae, and enterics.`
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 5,
    unitName: "Pathology & Diagnostics",
    category: "Diagnostic Medicine",
    description: "Cellular adaptations, inflammatory cascades, tissue repair, and neoplastic hallmarks.",
    isActive: false,
    modules: [
      {
        id: 501,
        unitId: 5,
        name: "Cell Injury & Hemodynamics",
        description: "Reversible cellular swelling, coagulative/liquefactive necrosis, and thrombosis.",
        topics: [
          {
            id: 5001,
            moduleId: 501,
            name: "Necrosis Patterns & Apoptosis",
            description: "Histological hallmarks of infarction, caseous granulomas, and enzyme leakage.",
            subtopics: [
              {
                id: 50001,
                topicId: 5001,
                name: "Morphologic Patterns of Tissue Necrosis",
                isCompleted: false,
                objectives: [
                  {
                    id: "obj-5-1",
                    title: "Six Classic Patterns of Necrosis",
                    description: "Identify Coagulative, Liquefactive, Caseous, Fat, Fibrinoid, and Gangrenous necrosis.",
                    content: `### The Six Patterns of Tissue Necrosis

1. **Coagulative Necrosis**: Seen in myocardial/renal infarcts. Preserved tissue architecture with ghost cells; denatured proteins prevent lysosomal breakdown.
2. **Liquefactive Necrosis**: Brain infarcts (due to high lipid content and microglial hydrolytic enzymes) and abscess cavities (neutrophilic enzymes).
3. **Caseous Necrosis**: "Cheese-like" amorphous debris characteristic of Mycobacterium tuberculosis and fungal granulomas.
4. **Fat Necrosis**: Acute pancreatitis (pancreatic lipases release fatty acids that saponify with calcium, forming chalky white deposits).
5. **Fibrinoid Necrosis**: Malignant hypertension and immune vasculitis (antigen-antibody complexes leak into arterial walls).
6. **Gangrenous Necrosis**: Dry (coagulative ischemia) vs Wet (superimposed bacterial liquefaction).`
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
];
