package de.tum.cit.fop.maze.entities.enemy;

enum EnemyState {
    IDLE,
    PATROL,
    CHASING,    // [新增] 追逐玩家 (触发 AggroPulse )
    ATTACK
}
