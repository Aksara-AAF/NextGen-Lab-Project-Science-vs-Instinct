package com.nextgenlab.game.command;

import com.nextgenlab.game.entity.Researcher;

public interface MoveCommand {
    void execute(Researcher researcher, float delta);
}
