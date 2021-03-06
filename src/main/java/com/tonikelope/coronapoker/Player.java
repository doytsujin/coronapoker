/*
 * Copyright (C) 2020 tonikelope
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tonikelope.coronapoker;

import javax.swing.JLabel;

/**
 *
 * @author tonikelope
 */
public interface Player {

    public static final int NODEC = -1;
    public static final int FOLD = 1;
    public static final int CHECK = 2;
    public static final int BET = 3;
    public static final int ALLIN = 4;

    public static final int DEALER = 11;
    public static final int SMALL_BLIND = 12;
    public static final int BIG_BLIND = 13;

    public static final int BORDER = 8;

    public boolean isTurno();

    public void resetBote();

    public boolean isWinner();

    public boolean isLoser();

    public void showCards(String jugada);

    public void setServer();

    public int getBuyin();

    public float getBote();

    public void setTimeout(boolean val);

    public String getNickname();

    public void setNickname(String name);

    public Card getPlayingCard1();

    public Card getPlayingCard2();

    public void setWinner(String msg);

    public void setLoser(String msg);

    public void setBoteSecundario(String msg);

    public void pagar(float pasta);

    public float getBet();

    void disableUTG();

    void refreshPos();

    public void disablePlayer(boolean visible);

    public void nuevaMano(int pos);

    public void esTuTurno();

    public int getDecision();

    public void setStack(float stack);

    public float getStack();

    public void setBet(float bet);

    public void resetBetDecision();

    public boolean isSpectator();

    public boolean isExit();

    public void setExit(boolean exit);

    public void reComprar(int cantidad);

    public String getLastActionString();

    public JLabel getAvatar();

    public void enableMantenimiento();

    public void disableMantenimiento();

    public void setBuyin(int buyin);

    public float getPagar();

    public void setSpectator(String msg);

    public void unsetSpectator();

    public int getPos();

}
