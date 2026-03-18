package com.example.pokemonbattle.server;

public class DamageMessage extends GameMessage {
    private static final long serialVersionUID = 1L;

    private Integer battleId;
    private String  attackerName;
    private String  targetName;
    private Integer damageDealt;
    private Integer targetCurrentHp;
    private Integer targetMaxHp;
    private Integer attackerCurrentHp;
    private Integer attackerMaxHp;
    private Integer healAmount;
    private boolean targetFainted;
    private Float   effectiveness;
    private String  moveUsed;

    public DamageMessage(Integer battleId, String attackerName, String targetName,
                         Integer damage, Integer targetCurrentHp, Integer targetMaxHp,
                         Integer attackerCurrentHp, Integer attackerMaxHp, Integer healAmount,
                         boolean fainted, Float effectiveness, String moveUsed) {
        super("DAMAGE");
        this.battleId        = battleId;
        this.attackerName    = attackerName;
        this.targetName      = targetName;
        this.damageDealt     = damage;
        this.targetCurrentHp = targetCurrentHp;
        this.targetMaxHp     = targetMaxHp;
        this.attackerCurrentHp = attackerCurrentHp;
        this.attackerMaxHp   = attackerMaxHp;
        this.healAmount      = healAmount;
        this.targetFainted   = fainted;
        this.effectiveness   = effectiveness;
        this.moveUsed        = moveUsed;
    }

    public Integer getBattleId()        { return battleId; }
    public String  getAttackerName()    { return attackerName; }
    public String  getTargetName()      { return targetName; }
    public Integer getDamageDealt()     { return damageDealt; }
    public Integer getTargetCurrentHp() { return targetCurrentHp; }
    public Integer getTargetMaxHp()     { return targetMaxHp; }
    public Integer getAttackerCurrentHp() { return attackerCurrentHp; }
    public Integer getAttackerMaxHp()   { return attackerMaxHp; }
    public Integer getHealAmount()      { return healAmount; }
    public boolean isTargetFainted()    { return targetFainted; }
    public Float   getEffectiveness()   { return effectiveness; }
    public String  getMoveUsed()        { return moveUsed; }
}
