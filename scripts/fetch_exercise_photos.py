"""
Download start/end photos for OwnFit's exercises from free-exercise-db
(https://github.com/yuhonas/free-exercise-db, Unlicense) and convert them to compact WebP.

  python scripts/fetch_exercise_photos.py

Writes app/src/main/assets/photos/<id>_0.webp, <id>_1.webp and photos/index.json.
Exercises without a good match keep the built-in vector animation.
"""
import io
import json
import os
import urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/"
OUT = os.path.join(ROOT, "app/src/main/assets/photos")
MAX_W = 560

# our id -> dataset exercise name (optionally "#0" to use only the start photo, e.g. a static hang)
MAP = {
    "arm_circles": "Arm Circles",
    "jumping_jacks": "Star Jump",
    "cat_cow_wu": "Cat Stretch",
    "cat_cow": "Cat Stretch",
    "inchworm": "Inchworm",
    "incline_pushup": "Incline Push-Up",
    "pushup": "Pushups",
    "wide_pushup": "Push-Up Wide",
    "archer_pushup": "Push-Up Wide",
    "decline_pushup": "Decline Push-Up",
    "diamond_pushup": "Close-Grip Push-Up off of a Dumbbell",
    "db_floor_press": "Dumbbell Floor Press",
    "db_bench_press": "Dumbbell Bench Press",
    "db_squeeze_press": "Close-Grip Dumbbell Press",
    "db_fly": "Dumbbell Flyes",
    "band_chest_press": "Standing Cable Chest Press",
    "superman": "Superman",
    "table_row": "Inverted Row",
    "db_row": "One-Arm Dumbbell Row",
    "db_bent_row": "Bent Over Two-Dumbbell Row",
    "db_reverse_fly": "Reverse Flyes",
    "band_pull_apart": "Band Pull Apart",
    "pullup": "Pullups",
    "chinup_negative": "Chin-Up",
    "dead_hang": "Pullups#0",
    "kb_swing": "One-Arm Kettlebell Swings",
    "db_shoulder_press": "Dumbbell Shoulder Press",
    "db_single_press": "Dumbbell One-Arm Shoulder Press",
    "db_lateral_raise": "Side Lateral Raise",
    "db_front_raise": "Front Dumbbell Raise",
    "db_arnold": "Arnold Dumbbell Press",
    "chair_dip": "Bench Dips",
    "db_curl": "Dumbbell Bicep Curl",
    "db_hammer_curl": "Hammer Curls",
    "db_conc_curl": "Concentration Curls",
    "db_overhead_ext": "Standing Dumbbell Triceps Extension",
    "db_kickback": "Tricep Dumbbell Kickback",
    "chair_squat": "Sit Squats",
    "squat": "Bodyweight Squat",
    "glute_bridge": "Butt Lift (Bridge)",
    "bridge_pose": "Butt Lift (Bridge)#1",
    "single_leg_bridge": "Single Leg Glute Bridge",
    "reverse_lunge": "Bodyweight Walking Lunge",
    "calf_raise": "Standing Calf Raises",
    "donkey_kick": "Glute Kickback",
    "side_leg_raise": "Side Leg Raises",
    "bulgarian_split": "Split Squats",
    "step_up": "Step-up with Knee Raise",
    "db_goblet_squat": "Goblet Squat",
    "db_rdl": "Stiff-Legged Dumbbell Deadlift",
    "db_lunge": "Dumbbell Lunges",
    "db_sumo_squat": "Plie Dumbbell Squat",
    "db_hip_thrust": "Barbell Hip Thrust",
    "band_lateral_walk": "Monster Walk",
    "dead_bug": "Dead Bug",
    "plank": "Plank",
    "side_plank": "Side Bridge",
    "crunch": "Crunches",
    "bicycle_crunch": "Air Bike",
    "leg_raise": "Flat Bench Lying Leg Raise",
    "russian_twist": "Russian Twist",
    "heel_taps": "Alternate Heel Touchers",
    "pallof_press": "Pallof Press",
    "db_woodchop": "Standing Cable Wood Chop",
    "hanging_knee_raise": "Hanging Leg Raise",
    "butt_kicks": "Single Leg Butt Kick",
    "mountain_climbers": "Mountain Climbers",
    "squat_jump": "Freehand Jump Squat",
    "skater": "Skating",
    "db_thruster": "Kettlebell Thruster",
    "hamstring_stretch": "Hamstring Stretch",
    "quad_stretch": "Quad Stretch",
    "chest_stretch": "Chest And Front Of Shoulder Stretch",
    "cross_body_stretch": "Shoulder Stretch",
    "figure_four": "Ankle On The Knee",
    "neck_release": "Side Neck Stretch",
    "triceps_stretch": "Triceps Stretch",
    "childs_pose": "Child's Pose",
    "low_lunge": "Kneeling Hip Flexor",
    "seated_twist": "Spinal Stretch",
}


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


def main():
    data = json.loads(fetch(BASE + "dist/exercises.json"))
    by_name = {x["name"]: x for x in data}
    os.makedirs(OUT, exist_ok=True)
    index = {}
    total = 0
    for ours, name in MAP.items():
        name, _, only = name.partition("#")
        ex = by_name.get(name)
        if not ex or not ex.get("images"):
            print("missing", ours, name)
            continue
        imgs = ex["images"][:2]
        if only:
            imgs = [imgs[int(only)]]
        for k, path in enumerate(imgs):
            im = Image.open(io.BytesIO(fetch(BASE + "exercises/" + path))).convert("RGB")
            if im.width > MAX_W:
                im = im.resize((MAX_W, round(im.height * MAX_W / im.width)), Image.LANCZOS)
            dest = os.path.join(OUT, f"{ours}_{k}.webp")
            im.save(dest, "WEBP", quality=72, method=6)
            total += os.path.getsize(dest)
        index[ours] = {"frames": len(imgs), "source": name}
    json.dump(index, open(os.path.join(OUT, "index.json"), "w"), indent=0)
    print(f"{len(index)} exercises, {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
