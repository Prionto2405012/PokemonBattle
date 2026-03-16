// BattleAnimationManager.java
package com.example.pokemonbattle.util;

import com.example.pokemonbattle.model.Move;
import com.example.pokemonbattle.util.effects.BugEffects;
import com.example.pokemonbattle.util.effects.DarkEffects;
import com.example.pokemonbattle.util.effects.DragonEffects;
import com.example.pokemonbattle.util.effects.ElectricEffects;
import com.example.pokemonbattle.util.effects.FairyEffects;
import com.example.pokemonbattle.util.effects.FightingEffects;
import com.example.pokemonbattle.util.effects.ContactOverlayEffects;
import com.example.pokemonbattle.util.effects.FireEffects;
import com.example.pokemonbattle.util.effects.FlyingEffects;
import com.example.pokemonbattle.util.effects.GhostEffects;
import com.example.pokemonbattle.util.effects.GrassEffects;
import com.example.pokemonbattle.util.effects.GroundEffects;
import com.example.pokemonbattle.util.effects.IceEffects;
import com.example.pokemonbattle.util.effects.PoisonEffects;
import com.example.pokemonbattle.util.effects.PsychicEffects;
import com.example.pokemonbattle.util.effects.RockEffects;
import com.example.pokemonbattle.util.effects.SteelEffects;
import com.example.pokemonbattle.util.effects.WaterEffects;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class BattleAnimationManager {

    private final ImageView playerSprite;
    private final Pane battleField;
    private boolean animationEnabled = true;

    // Animation constants
    private static final double ATTACK_DISTANCE_FULL   = 120.0;
    private static final double ATTACK_DISTANCE_SLIGHT = 30.0;
    private static final double ATTACK_DURATION_MS     = 480.0;
    private static final double RETURN_DURATION_MS     = 550.0;
    private static final double IMPACT_SHAKE_DISTANCE  = 8.0;
    private static final double IMPACT_SCALE_REDUCTION = 0.88;
    private static final double ANIMATION_SPEED_MULTIPLIER = 1.2;

    // Type effect handlers
    private final ElectricEffects electricEffects;
    private final IceEffects      iceEffects;
    private final FireEffects     fireEffects;
    private final FightingEffects fightingEffects;
    private final ContactOverlayEffects contactOverlayEffects;
    private final RockEffects     rockEffects;
    private final WaterEffects    waterEffects;
    private final GhostEffects    ghostEffects;
    private final PsychicEffects  psychicEffects;
    private final DarkEffects     darkEffects;
    private final GroundEffects   groundEffects;
    private final FlyingEffects   flyingEffects;
    private final PoisonEffects   poisonEffects;
    private final FairyEffects    fairyEffects;
    private final SteelEffects    steelEffects;
    private final DragonEffects   dragonEffects;
    private final BugEffects      bugEffects;
    private final GrassEffects    grassEffects;

    public BattleAnimationManager(ImageView playerSprite, ImageView opponentSprite, Pane battleField) {
        this.playerSprite   = playerSprite;
        this.battleField    = battleField;

        this.electricEffects = new ElectricEffects(battleField);
        this.iceEffects      = new IceEffects(battleField);
        this.fireEffects     = new FireEffects(battleField);
        this.fightingEffects = new FightingEffects(battleField);
        this.contactOverlayEffects = new ContactOverlayEffects(battleField);
        this.rockEffects     = new RockEffects(battleField);
        this.waterEffects    = new WaterEffects(battleField);
        this.ghostEffects    = new GhostEffects(battleField);
        this.psychicEffects  = new PsychicEffects(battleField);
        this.darkEffects     = new DarkEffects(battleField);
        this.groundEffects   = new GroundEffects(battleField);
        this.flyingEffects   = new FlyingEffects(battleField);
        this.poisonEffects   = new PoisonEffects(battleField);
        this.fairyEffects    = new FairyEffects(battleField);
        this.steelEffects    = new SteelEffects(battleField);
        this.dragonEffects   = new DragonEffects(battleField);
        this.bugEffects      = new BugEffects(battleField);
        this.grassEffects    = new GrassEffects(battleField);
    }

    // =
    // PUBLIC API
    // =

    public void playAttackAnimation(ImageView attacker, ImageView defender, Move move, Runnable onComplete) {
        // If animation is disabled, skip all movement/effects and just call onComplete
        if (!animationEnabled) {
            if (onComplete != null) onComplete.run();
            return;
        }

        double attackerOriginalX      = attacker.getTranslateX();
        double attackerOriginalY      = attacker.getTranslateY();
        double attackerOriginalScaleX = attacker.getScaleX();
        double attackerOriginalScaleY = attacker.getScaleY();
        double defenderOriginalX      = defender.getTranslateX();
        double defenderOriginalY      = defender.getTranslateY();
        double defenderOriginalScaleX = defender.getScaleX();
        double defenderOriginalScaleY = defender.getScaleY();

        String  moveName    = (move != null && move.getName() != null) ? move.getName().toLowerCase() : "tackle";
        String  moveType    = (move != null && move.getType() != null) ? move.getType().toLowerCase() : "normal";
        String  damageClass = (move != null && move.getDamage_class() != null)
            ? move.getDamage_class().toLowerCase()
            : "physical";
        Integer powerVal    = (move != null) ? move.getPower() : null;
        int     movePower   = (powerVal != null) ? powerVal : 50;

        double attackerX = getCenterX(attacker);
        double attackerY = getCenterY(attacker);
        double defenderX = getCenterX(defender);
        boolean attackingRight = attackerX <= defenderX;

        double attackDistance = getAttackDistance(moveType, moveName, damageClass);
        attackDistance = attackingRight ? attackDistance : -attackDistance;

        // Ranged / special-movement moves bypass the standard melee sequence
        if (isRangedMove(moveName, moveType, damageClass)) {
            playRangedAnimation(attacker, defender, moveName, moveType, movePower,
                    defenderOriginalX, defenderOriginalY,
                    defenderOriginalScaleX, defenderOriginalScaleY, onComplete);
            return;
        }

        // ── Standard melee sequence ───────────────────────────────────────────
        TranslateTransition rush = new TranslateTransition(Duration.millis(ATTACK_DURATION_MS), attacker);
        rush.setByX(attackDistance);
        rush.setInterpolator(Interpolator.EASE_IN);

        Timeline movementEffect = createMovementEffect(attacker, moveType, attackingRight, moveName);

        ParallelTransition impact = createImpactEffect(defender, attackerX, attackerY,
                moveName, moveType, movePower, defenderOriginalX);

        TranslateTransition retreat = new TranslateTransition(Duration.millis(RETURN_DURATION_MS), attacker);
        retreat.setToX(attackerOriginalX);
        retreat.setToY(attackerOriginalY);
        retreat.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition recovery = createRecoveryEffect(
                defender, defenderOriginalX, defenderOriginalY,
                defenderOriginalScaleX, defenderOriginalScaleY);

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(rush, movementEffect),
                impact,
                new ParallelTransition(retreat, recovery));

        sequence.setOnFinished(e -> {
            attacker.setTranslateX(attackerOriginalX);
            attacker.setTranslateY(attackerOriginalY);
            attacker.setScaleX(attackerOriginalScaleX);
            attacker.setScaleY(attackerOriginalScaleY);
            defender.setScaleX(defenderOriginalScaleX);
            defender.setScaleY(defenderOriginalScaleY);
            defender.setTranslateX(defenderOriginalX);
            defender.setTranslateY(defenderOriginalY);
            if (onComplete != null) onComplete.run();
        });

        playAtCurrentSpeed(sequence);
    }

    // =
    // DISTANCE / RANGED ROUTING
    // =

    private double getAttackDistance(String moveType, String moveName, String damageClass) {
        // Ice: melee punch closes gap fully, everything else lunges slightly
        if (moveType.equals("ice")) {
            return moveName.contains("punch") ? ATTACK_DISTANCE_FULL : ATTACK_DISTANCE_SLIGHT;
        }

        // Fire: named melee moves close gap fully, all others lunge slightly
        if (moveType.equals("fire")) {
            return switch (moveName) {
                case "fire-punch", "fire-fang", "blaze-kick", "flare-blitz",
                     "flame-charge", "flame-wheel", "raging-fury",
                     "flame-burst", "temper-flare" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Water: crabhammer is the only true melee
        if (moveType.equals("water")) {
            return moveName.equals("crabhammer") ? ATTACK_DISTANCE_FULL : ATTACK_DISTANCE_SLIGHT;
        }

        // Ghost: phasing melee moves close gap, ranged moves stay slight
        if (moveType.equals("ghost")) {
            return switch (moveName) {
                case "astonish", "lick", "phantom-force", "shadow-claw",
                     "shadow-force", "shadow-punch", "shadow-sneak" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Psychic: telekinetic melee moves close gap, all others slight
        if (moveType.equals("psychic")) {
            return switch (moveName) {
                case "heart-stamp", "psychic-fangs", "psycho-cut",
                     "psyshield-bash", "psystrike", "zen-headbutt" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Dark: melee lunge moves close gap, ranged/delayed stay slight
        if (moveType.equals("dark")) {
            return switch (moveName) {
                case "bite", "crunch", "foul-play", "knock-off", "lash-out",
                     "night-slash", "pursuit", "sucker-punch", "thief",
                     "throat-chop", "brutal-swing", "darkest-lariat",
                     "feint-attack" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Ground: melee-like physical moves close gap, ranged stay slight
        if (moveType.equals("ground")) {
            return switch (moveName) {
                case "bone-rush", "bonemerang", "bone-club", "bulldoze",
                     "drill-run", "high-horsepower", "headlong-rush",
                     "stomping-tantrum", "dig" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Flying: melee dive/wing moves close gap; wind/beam moves slight
        if (moveType.equals("flying")) {
            return switch (moveName) {
                case "wing-attack", "aerial-ace", "fly", "sky-attack", "bounce",
                     "brave-bird", "dual-wingbeat", "peck", "drill-peck" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Poison: physical contact moves close gap; sludge/projectile slight
        if (moveType.equals("poison")) {
            return switch (moveName) {
                case "poison-jab", "cross-poison", "poison-fang",
                     "venom-drench", "poison-tail" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Fairy: play-rough and draining-kiss close gap; sparkle/beam slight
        if (moveType.equals("fairy")) {
            return switch (moveName) {
                case "play-rough", "spirit-break", "draining-kiss" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Steel: heavy iron-head style moves close gap; cannon/projectile slight
        if (moveType.equals("steel")) {
            return switch (moveName) {
                case "iron-head", "iron-tail", "bullet-punch", "meteor-mash",
                     "smart-strike", "steel-wing", "heavy-slam" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Dragon: claw/rush moves close gap; pulse/meteor slight
        if (moveType.equals("dragon")) {
            return switch (moveName) {
                case "dragon-claw", "spacial-rend", "dual-chop", "breaking-swipe",
                     "dragon-rush", "outrage" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Bug: x-scissor and physical bite/leech moves close gap; buzz/beam slight
        if (moveType.equals("bug")) {
            return switch (moveName) {
                case "x-scissor", "fury-cutter", "twineedle", "lunge",
                     "bug-bite", "leech-life" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Grass: vine/leaf/wood melee moves close gap; beam/seed slight
        if (moveType.equals("grass")) {
            return switch (moveName) {
                case "vine-whip", "power-whip", "wood-hammer", "leaf-blade",
                     "petal-dance" -> ATTACK_DISTANCE_FULL;
                default -> ATTACK_DISTANCE_SLIGHT;
            };
        }

        // Rock: rock-tomb uses a slight advance (rocks fall from above, not from attacker)
        if (moveType.equals("rock")) {
            return switch (moveName) {
                case "rock-tomb" -> ATTACK_DISTANCE_SLIGHT;
                default -> ATTACK_DISTANCE_FULL;
            };
        }

        // Ranged moves don't move the attacker at all (handled via playRangedAnimation)
        if (isRangedMove(moveName, moveType, damageClass)) return 0;

        return ATTACK_DISTANCE_FULL;
    }

    /**
     * Returns true for moves whose primary visual travels from attacker to defender
     * as a projectile/beam/wave — the attacker sprite does NOT rush across.
     */
    private boolean isRangedMove(String moveName, String moveType, String damageClass) {
        // Ice beam-type moves
        if (moveName.contains("beam") || moveName.contains("spear")) return true;

        return switch (moveType) {
            // Fire ranged / area moves — attacker only lunges slightly
            case "fire" -> switch (moveName) {
                case "flamethrower", "overheat", "heat-wave", "blast-burn",
                     "mystical-fire", "ember", "fire-spin", "magma-storm",
                     "lava-plume", "eruption", "inferno", "fire-pledge",
                     "fire-blast", "incinerate", "burning-jealousy",
                     "sacred-fire", "burn-up" -> true;
                default -> false;
            };
            case "water" -> !moveName.equals("crabhammer");
            case "electric" -> damageClass.equals("special");
            case "rock" -> switch (moveName) {
                case "rock-blast", "rock-slide", "rock-throw",
                     "rock-wrecker", "power-gem", "meteor-beam",
                     "ancient-power", "smack-down" -> true;
                default -> false;
            };
            // Ghost: shadow-ball and ominous-wind are ranged; hex/poltergeist/rage-fist
            // are delayed-burst (also ranged-like)
            case "ghost" -> switch (moveName) {
                case "shadow-ball", "ominous-wind", "hex", "poltergeist",
                     "rage-fist" -> true;
                default -> false;
            };
            // Psychic: beam moves, ring moves, and aura-burst moves are ranged
            case "psychic" -> switch (moveName) {
                case "psybeam", "twin-beam", "confusion", "expanding-force",
                     "extrasensory", "luster-purge", "mist-ball",
                     "mystical-power", "psychic", "psychic-noise",
                     "psycho-boost", "psyshock", "synchronoise",
                     "dream-eater", "future-sight", "lunar-blessing",
                     "stored-power", "take-heart" -> true;
                default -> false;
            };
            // Dark: pulse/snarl and delayed-retaliation moves are ranged
            case "dark" -> switch (moveName) {
                case "dark-pulse", "snarl", "assurance", "comeuppance",
                     "payback", "power-trip" -> true;
                default -> false;
            };
            // Ground: ranged projectile and eruption/sand moves
            case "ground" -> switch (moveName) {
                case "earth-power", "lands-wrath", "land's-wrath",
                     "precipice-blades", "mud-bomb", "mud-shot",
                     "sand-attack", "sand-tomb", "sandstorm",
                     "scorching-sands", "earthquake" -> true;
                default -> false;
            };
            // Flying: wind/beam/dive ranged moves
            case "flying" -> switch (moveName) {
                case "air-slash", "air-cutter", "hurricane", "gust",
                     "tailwind", "oblivion-wing", "bleakwind-storm" -> true;
                default -> false;
            };
            // Poison: sludge/acid/toxic ranged moves
            case "poison" -> switch (moveName) {
                case "sludge", "sludge-bomb", "sludge-wave", "acid",
                     "acid-spray", "gunk-shot", "toxic-spikes",
                     "clear-smog", "belch" -> true;
                default -> false;
            };
            // Fairy: sparkle/moonblast/gleam ranged moves
            case "fairy" -> switch (moveName) {
                case "moonblast", "dazzling-gleam", "disarming-voice",
                     "moongeist-beam", "misty-explosion", "sparkling-aria",
                     "strange-steam", "fairy-wind", "charm" -> true;
                default -> false;
            };
            // Steel: flash-cannon/gyro-ball/magnet-bomb ranged moves
            case "steel" -> switch (moveName) {
                case "flash-cannon", "magnet-bomb", "anchor-shot",
                     "gyro-ball", "gear-grind" -> true;
                default -> false;
            };
            // Dragon: pulse/breath/meteor ranged moves
            case "dragon" -> switch (moveName) {
                case "dragon-pulse", "dragon-breath", "dragon-rage",
                     "draco-meteor", "scale-shot" -> true;
                default -> false;
            };
            // Bug: buzz/signal/silver-wind ranged moves
            case "bug" -> switch (moveName) {
                case "bug-buzz", "signal-beam", "silver-wind",
                     "pollen-puff", "infestation", "attack-order" -> true;
                default -> false;
            };
            // Grass: seed/beam/leaf ranged moves
            case "grass" -> switch (moveName) {
                case "razor-leaf", "bullet-seed", "seed-bomb", "magical-leaf",
                     "petal-blizzard", "energy-ball", "leaf-storm",
                     "solar-beam", "seed-flare", "frenzy-plant" -> true;
                default -> false;
            };
            default -> false;
        };
    }

    // =
    // RANGED ANIMATION PATH
    // =

    private void playRangedAnimation(ImageView attacker, ImageView defender,
            String moveName, String moveType, int movePower,
            double defenderOriginalX, double defenderOriginalY,
            double defenderOriginalScaleX, double defenderOriginalScaleY,
            Runnable onComplete) {

        double attackerX = getCenterX(attacker);
        double attackerY = getCenterY(attacker);
        double defenderX = getCenterX(defender);
        double defenderY = getCenterY(defender);

        Animation leadEffect = null;

        switch (moveType) {
            case "ice" -> leadEffect = iceEffects.createBeamEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower);
            case "electric" -> leadEffect = electricEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower);
            case "water" -> {
            Timeline wt = new Timeline();
            waterEffects.createImpactEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, wt);
            if (moveName.equals("dive")) {
                // ── Dive: attacker sinks underground then re-emerges near defender ──
                double origOpacity = attacker.getOpacity();
                double origTransX  = attacker.getTranslateX();
                double origTransY  = attacker.getTranslateY();

                // Phase 1: attacker gradually sinks (fades out + slides down a bit)
                Timeline sinkAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(attacker.opacityProperty(), origOpacity)),
                    new KeyFrame(Duration.millis(380),
                        new KeyValue(attacker.opacityProperty(), 0.0),
                        new KeyValue(attacker.translateYProperty(), origTransY + 25))
                );

                // Phase 2: teleport attacker near defender (still invisible)
                //          then fade back in
                boolean isPlayer = (attacker == playerSprite);
                double emergeOffsetX = isPlayer ? -50 : 50;
                Timeline emergeAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(attacker.translateXProperty(),
                            defenderX - attackerX + emergeOffsetX),
                        new KeyValue(attacker.translateYProperty(),
                            defenderY - attackerY),
                        new KeyValue(attacker.opacityProperty(), 0.0)),
                    new KeyFrame(Duration.millis(300),
                        new KeyValue(attacker.opacityProperty(), origOpacity))
                );

                // Phase 3: return attacker to original position (after impact)
                Timeline returnAnim = new Timeline(
                    new KeyFrame(Duration.millis(160),
                        new KeyValue(attacker.translateXProperty(), origTransX),
                        new KeyValue(attacker.translateYProperty(), origTransY))
                );

                // Combine: [water fx + sink] → emerge → (impact handled by caller) → return
                ParallelTransition sinkPhase = new ParallelTransition(wt, sinkAnim);
                SequentialTransition diveSeq = new SequentialTransition(
                    sinkPhase, emergeAnim, returnAnim);
                leadEffect = diveSeq;
            } else {
                leadEffect = wt;
            }
            }
            case "fire" -> {
            Timeline ft = new Timeline();
            fireEffects.createImpactEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, ft);
            leadEffect = ft;
            }
            case "rock" -> {
            Timeline rt = new Timeline();
            rockEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, rt);
            leadEffect = rt;
            }
            case "ghost" -> {
            Timeline gt = new Timeline();
            ghostEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, gt);
            leadEffect = gt;
            }
            case "psychic" -> {
            Timeline pt = new Timeline();
            psychicEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, pt);
            leadEffect = pt;
            }
            case "dark" -> {
            Timeline dt = new Timeline();
            darkEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, dt);
            leadEffect = dt;
            }
            case "ground" -> {
            Timeline grt = new Timeline();
            groundEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, grt);
            leadEffect = grt;
            }
            case "flying" -> {
            Timeline flyingTimeline = new Timeline();
            flyingEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, flyingTimeline);
            leadEffect = flyingTimeline;
            }
            case "poison" -> {
            Timeline poisonTimeline = new Timeline();
            poisonEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, poisonTimeline);
            leadEffect = poisonTimeline;
            }
            case "fairy" -> {
            Timeline fairyTimeline = new Timeline();
            fairyEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, fairyTimeline);
            leadEffect = fairyTimeline;
            }
            case "steel" -> {
            Timeline steelTimeline = new Timeline();
            steelEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, steelTimeline);
            leadEffect = steelTimeline;
            }
            case "dragon" -> {
            Timeline dragonTimeline = new Timeline();
            dragonEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, dragonTimeline);
            leadEffect = dragonTimeline;
            }
            case "bug" -> {
            Timeline bugTimeline = new Timeline();
            bugEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, bugTimeline);
            leadEffect = bugTimeline;
            }
            case "grass" -> {
            Timeline grassTimeline = new Timeline();
            grassEffects.createRangedEffect(
                attackerX, attackerY, defenderX, defenderY, moveName, movePower, grassTimeline);
            leadEffect = grassTimeline;
            }
            default -> {
            }
        }

        if (leadEffect != null) {
            EventHandler<ActionEvent> previous = leadEffect.getOnFinished();
            leadEffect.setOnFinished(e -> {
                if (previous != null) previous.handle(e);

                ParallelTransition impact = createImpactEffect(
                        defender, attackerX, attackerY,
                        moveName, moveType, movePower, defenderOriginalX);
                ParallelTransition recovery = createRecoveryEffect(
                        defender, defenderOriginalX, defenderOriginalY,
                        defenderOriginalScaleX, defenderOriginalScaleY);

                SequentialTransition seq = new SequentialTransition(impact, recovery);
                seq.setOnFinished(ev -> {
                    defender.setScaleX(defenderOriginalScaleX);
                    defender.setScaleY(defenderOriginalScaleY);
                    defender.setTranslateX(defenderOriginalX);
                    defender.setTranslateY(defenderOriginalY);
                    if (onComplete != null) onComplete.run();
                });
                playAtCurrentSpeed(seq);
            });
            playAtCurrentSpeed(leadEffect);
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    // =
    // MOVEMENT EFFECT (during attacker rush)
    // =

    private Timeline createMovementEffect(ImageView attacker, String moveType,
            boolean attackingRight, String moveName) {
        Timeline effect = new Timeline();

        double attackerX = attacker.getLayoutX() + attacker.getFitWidth()  / 2;
        double attackerY = attacker.getLayoutY() + attacker.getFitHeight() / 2;

        if (moveType.equals("electric")) {
            electricEffects.addMovementSparks(attackerX, attackerY, attackingRight, effect);
        } else if (moveName.equals("flare-blitz") || moveName.equals("flame-wheel")
                || moveName.equals("flame-charge")) {
            fireEffects.addChargeTrailForMove(moveName, attackerX, attackerY, attackingRight, effect);
        }

        return effect;
    }

    // =
    // IMPACT EFFECT
    // =

    private ParallelTransition createImpactEffect(ImageView defender,
            double attackerX, double attackerY,
            String moveName, String moveType, int movePower,
            double defenderBaseTranslateX) {

        Timeline shake = createShakeEffect(defender, movePower, defenderBaseTranslateX);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(110), defender);
        shrink.setToX(IMPACT_SCALE_REDUCTION);
        shrink.setToY(IMPACT_SCALE_REDUCTION);
        shrink.setAutoReverse(true);
        shrink.setCycleCount(2);

        ColorAdjust flash = new ColorAdjust();
        defender.setEffect(flash);
        Timeline flashTimeline = createFlashEffect(flash, movePower);

        double fieldMidX = battleField.getWidth() > 0 ? battleField.getWidth() / 2.0 : 0.0;
        double defenderMidX = getCenterX(defender);
        double pushDirection = defenderMidX <= fieldMidX ? -1.0 : 1.0;
        double pushDistance = Math.min(10 + movePower / 18.0, 22.0);
        TranslateTransition knockback = new TranslateTransition(Duration.millis(160), defender);
        knockback.setToX(defenderBaseTranslateX + pushDirection * pushDistance);
        knockback.setInterpolator(Interpolator.EASE_OUT);

        double defenderX = getCenterX(defender);
        double defenderY = getCenterY(defender);

        Timeline typeEffect = createTypeSpecificImpact(
                attackerX, attackerY, defenderX, defenderY,
                moveName, moveType, movePower);

        ParallelTransition impact = new ParallelTransition(
                shake, shrink, flashTimeline, typeEffect, knockback);
        impact.setOnFinished(e -> defender.setEffect(null));

        return impact;
    }

    // =
    // SHAKE / FLASH
    // =

    private Timeline createShakeEffect(ImageView defender, int movePower, double defenderBaseTranslateX) {
        double shakeIntensity = Math.min(IMPACT_SHAKE_DISTANCE * (movePower / 80.0), 15.0);
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX)),
                new KeyFrame(Duration.millis(55),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX + shakeIntensity)),
                new KeyFrame(Duration.millis(110),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX - shakeIntensity)),
                new KeyFrame(Duration.millis(165),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX + shakeIntensity)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(defender.translateXProperty(), defenderBaseTranslateX)));
    }

    private Timeline createFlashEffect(ColorAdjust flash, int movePower) {
        double flashIntensity = Math.min(0.9, 0.6 + movePower / 200.0);
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(flash.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(70),
                        new KeyValue(flash.brightnessProperty(), flashIntensity)),
                new KeyFrame(Duration.millis(140),
                        new KeyValue(flash.brightnessProperty(), 0)),
                new KeyFrame(Duration.millis(210),
                        new KeyValue(flash.brightnessProperty(), flashIntensity * 0.7)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(flash.brightnessProperty(), 0)));
    }

    // =
    // TYPE-SPECIFIC IMPACT DISPATCH
    // =

    private Timeline createTypeSpecificImpact(double startX, double startY,
            double endX, double endY,
            String moveName, String moveType, int movePower) {

        Timeline effect = new Timeline();

        switch (moveType) {
            case "electric" -> electricEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "ice"      -> iceEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fire"     -> fireEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fighting" -> fightingEffects.createImpactEffect(endX, endY, moveName, movePower, effect);
            case "water"    -> waterEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "rock"     -> rockEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "ghost"    -> ghostEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "psychic"  -> psychicEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "dark"     -> darkEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "ground"   -> groundEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "flying"   -> flyingEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "poison"   -> poisonEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "fairy"    -> fairyEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "steel"    -> steelEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "dragon"   -> dragonEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "bug"      -> bugEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            case "grass"    -> grassEffects.createImpactEffect(startX, startY, endX, endY, moveName, movePower, effect);
            default         -> createDefaultImpact(endX, endY, movePower, effect);
        }

        if (isFangMove(moveName)) {
            contactOverlayEffects.addFangAnimation(endX, endY, effect);
        }

        if (!moveType.equals("fighting") && isKickOrFeetMove(moveName)) {
            contactOverlayEffects.addFeetImage(endX, endY, effect);
        }

        // For non-fighting punch moves (fire-punch, ice-punch, thunder-punch),
        // overlay the punch image + elemental particles on top of the type effect
        if (!moveType.equals("fighting") && moveName.contains("punch")) {
            fightingEffects.addPunchImageAndOverlay(endX, endY, moveName, moveType, effect);
        }

        return effect;
    }

    // =
    // DEFAULT IMPACT (white particles)
    // =

    private void createDefaultImpact(double x, double y, int movePower, Timeline timeline) {
        int particleCount = Math.min(3 + movePower / 20, 10);

        for (int i = 0; i < particleCount; i++) {
            Circle particle = new Circle(6, javafx.scene.paint.Color.WHITE);
            prepareTransientNode(particle);
            particle.setOpacity(0);

            double angle  = (i / (double) particleCount) * 2 * Math.PI;
            double radius = 30 + movePower / 4.0;

            particle.setCenterX(x);
            particle.setCenterY(y);
            battleField.getChildren().add(particle);

            KeyFrame appear = new KeyFrame(Duration.millis(70),
                    new KeyValue(particle.opacityProperty(), 1.0),
                    new KeyValue(particle.radiusProperty(), 8));
            KeyFrame expand = new KeyFrame(Duration.millis(200),
                    new KeyValue(particle.centerXProperty(), x + Math.cos(angle) * radius),
                    new KeyValue(particle.centerYProperty(), y + Math.sin(angle) * radius),
                    new KeyValue(particle.radiusProperty(), 3));
            KeyFrame fade = new KeyFrame(Duration.millis(280),
                    new KeyValue(particle.opacityProperty(), 0));

            timeline.getKeyFrames().addAll(appear, expand, fade);
            registerCleanup(timeline, particle);
        }
    }

    // RECOVERY

    private ParallelTransition createRecoveryEffect(ImageView defender,
            double baseTranslateX, double baseTranslateY,
            double baseScaleX, double baseScaleY) {

        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(200), defender);
        scaleBack.setToX(baseScaleX);
        scaleBack.setToY(baseScaleY);

        TranslateTransition slideBack = new TranslateTransition(Duration.millis(200), defender);
        slideBack.setToX(baseTranslateX);
        slideBack.setToY(baseTranslateY);

        return new ParallelTransition(scaleBack, slideBack);
    }

    // =
    // HELPERS
    // =

    private void prepareTransientNode(javafx.scene.Node node) {
        node.setManaged(false);
        node.setMouseTransparent(true);
    }

    private boolean isKickOrFeetMove(String moveName) {
        return moveName.contains("-kick") || moveName.contains("-feet");
    }

    private boolean isFangMove(String moveName) {
        return moveName.contains("fang")
                || moveName.equals("bite")
                || moveName.endsWith("-bite")
                || moveName.equals("crunch")
                || moveName.equals("leech-life");
    }

    private void registerCleanup(Timeline timeline, javafx.scene.Node node) {
        EventHandler<ActionEvent> previous = timeline.getOnFinished();
        timeline.setOnFinished(e -> {
            battleField.getChildren().remove(node);
            if (previous != null) previous.handle(e);
        });
    }

    private double getCenterX(ImageView sprite) {
        Bounds b = sprite.getBoundsInParent();
        return b.getMinX() + b.getWidth() / 2.0;
    }

    private double getCenterY(ImageView sprite) {
        Bounds b = sprite.getBoundsInParent();
        return b.getMinY() + b.getHeight() / 2.0;
    }

    // =
    // ANIMATION TOGGLE
    // =

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    private void playAtCurrentSpeed(Animation animation) {
        animation.setRate(ANIMATION_SPEED_MULTIPLIER);
        animation.play();
    }

    // =
    // FLOATING DAMAGE / HEAL NUMBERS
    // =

    /**
     * Shows a floating damage number (red) beside the defender sprite.
     * Fades in, holds briefly, then fades out over ~1.8s total.
     */
    public void showDamageNumber(ImageView defender, int damage) {
        if (damage <= 0) return;

        Text damageText = new Text("-" + damage);
        damageText.setFont(Font.font("System", FontWeight.BOLD, 22));
        damageText.setFill(Color.RED);
        damageText.setStroke(Color.DARKRED);
        damageText.setStrokeWidth(0.8);
        damageText.setEffect(new DropShadow(4, Color.BLACK));
        prepareTransientNode(damageText);
        damageText.setOpacity(0);

        double x = defender.getLayoutX() + defender.getFitWidth() + 5;
        double y = defender.getLayoutY() + defender.getFitHeight() * 0.3;
        damageText.setX(x);
        damageText.setY(y);

        battleField.getChildren().add(damageText);

        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(damageText.opacityProperty(), 0),
                new KeyValue(damageText.yProperty(), y)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(damageText.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(1200),
                new KeyValue(damageText.opacityProperty(), 1.0),
                new KeyValue(damageText.yProperty(), y - 15)),
            new KeyFrame(Duration.millis(1800),
                new KeyValue(damageText.opacityProperty(), 0),
                new KeyValue(damageText.yProperty(), y - 25))
        );
        anim.setOnFinished(e -> battleField.getChildren().remove(damageText));
        playAtCurrentSpeed(anim);
    }

    /**
     * Shows a floating heal number (green) beside the attacker sprite.
     * Fades in, holds briefly, then fades out over ~1.8s total.
     */
    public void showHealNumber(ImageView target, int healAmount) {
        if (healAmount <= 0) return;

        Text healText = new Text("+" + healAmount);
        healText.setFont(Font.font("System", FontWeight.BOLD, 22));
        healText.setFill(Color.LIMEGREEN);
        healText.setStroke(Color.DARKGREEN);
        healText.setStrokeWidth(0.8);
        healText.setEffect(new DropShadow(4, Color.BLACK));
        prepareTransientNode(healText);
        healText.setOpacity(0);

        double x = target.getLayoutX() + target.getFitWidth() + 5;
        double y = target.getLayoutY() + target.getFitHeight() * 0.3;
        healText.setX(x);
        healText.setY(y);

        battleField.getChildren().add(healText);

        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(healText.opacityProperty(), 0),
                new KeyValue(healText.yProperty(), y)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(healText.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(1200),
                new KeyValue(healText.opacityProperty(), 1.0),
                new KeyValue(healText.yProperty(), y - 15)),
            new KeyFrame(Duration.millis(1800),
                new KeyValue(healText.opacityProperty(), 0),
                new KeyValue(healText.yProperty(), y - 25))
        );
        anim.setOnFinished(e -> battleField.getChildren().remove(healText));
        playAtCurrentSpeed(anim);
    }
}