package com.tonikelope.coronapoker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

/**
 *
 * @author tonikelope
 */
public class LocalPlayer extends JPanel implements ZoomableInterface, Player {

    public static final String[][] ACTIONS_LABELS_ES = new String[][]{new String[]{"NO VAS"}, new String[]{"PASAS", "VAS"}, new String[]{"APUESTAS", "SUBES"}, new String[]{"ALL IN"}};
    public static final String[][] ACTIONS_LABELS_EN = new String[][]{new String[]{"FOLD"}, new String[]{"CHECK", "CALL"}, new String[]{"BET", "RAISE"}, new String[]{"ALL IN"}};
    public static String[][] ACTIONS_LABELS = Game.LANGUAGE.equals("es") ? ACTIONS_LABELS_ES : ACTIONS_LABELS_EN;
    public static final String[] POSITIONS_LABELS_ES = new String[]{"CP", "CG", "DE"};
    public static final String[] POSITIONS_LABELS_EN = new String[]{"SB", "BB", "DE"};
    public static String[] POSITIONS_LABELS = Game.LANGUAGE.equals("es") ? POSITIONS_LABELS_ES : POSITIONS_LABELS_EN;
    public static final Color[][] ACTIONS_COLORS = new Color[][]{new Color[]{Color.GRAY, Color.WHITE}, new Color[]{Color.WHITE, Color.BLACK}, new Color[]{Color.ORANGE, Color.BLACK}, new Color[]{Color.BLACK, Color.WHITE}};
    public static final int MIN_ACTION_WIDTH = 550;
    public static final int MIN_ACTION_HEIGHT = 45;

    private String nickname;
    private float stack;
    private int buyin = Game.BUYIN;
    private float bet;
    private boolean utg = false;
    private volatile int decision = -1;
    private Crupier crupier = null;
    private volatile boolean spectator = false;
    private float pagar = 0f;
    private float bote = 0f;
    private volatile boolean exit = false;
    private volatile boolean turno = false;
    private volatile Timer auto_action = null;
    private volatile boolean timeout_val = false;
    private volatile boolean boton_mostrar = false;
    private volatile boolean winner = false;
    private volatile boolean loser = false;
    private volatile Float apuesta_recuperada = null;
    private volatile boolean click_recuperacion = false;
    private volatile int pos;
    private float call_required;
    private float min_raise;
    private final ConcurrentHashMap<JButton, Color[]> action_button_colors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JButton, Boolean> action_button_armed = new ConcurrentHashMap<>();
    private volatile int pre_pulsado = Player.NODEC;
    private final Object pre_pulsar_lock = new Object();
    private volatile boolean muestra = false;
    private volatile float slider_divisor = 10f;
    private volatile boolean playing_animation = false;
    private volatile int parguela_counter = Game.PARGUELA_COUNTER;

    public int getPos() {
        return pos;
    }

    public boolean isTurno() {
        return turno;
    }

    public int getParguela_counter() {
        return parguela_counter;
    }

    public void updateParguela_counter() {
        this.parguela_counter--;
    }

    public boolean isPlaying_animation() {
        return playing_animation;
    }

    public static void translateActionLabels() {

        ACTIONS_LABELS = Game.LANGUAGE.equals("es") ? ACTIONS_LABELS_ES : ACTIONS_LABELS_EN;
    }

    public void setClick_recuperacion(boolean click_recuperacion) {
        this.click_recuperacion = click_recuperacion;
    }

    public void setApuesta_recuperada(Float apuesta_recuperada) {
        this.apuesta_recuperada = apuesta_recuperada;
    }

    public JTextField getBet_slider_text() {
        return bet_slider_text;
    }

    public JButton getPlayer_allin_button() {
        return player_allin_button;
    }

    public JButton getPlayer_check_button() {
        return player_check_button;
    }

    public JButton getPlayer_fold_button() {
        return player_fold_button;
    }

    public void setMuestra(boolean muestra) {
        this.muestra = muestra;
    }

    public boolean isMuestra() {
        return muestra;
    }

    public boolean isWinner() {
        return winner;
    }

    public boolean isLoser() {
        return loser;
    }

    public JSlider getBet_slider() {
        return bet_slider;
    }

    public void activar_boton_mostrar(boolean parguela) {

        boton_mostrar = true;

        desactivarControles();

        Helpers.GUIRun(new Runnable() {
            public void run() {
                if (parguela) {
                    player_allin_button.setText(Translator.translate("MOSTRAR [F5]") + " (" + parguela_counter + ")");
                } else {
                    player_allin_button.setText(Translator.translate("MOSTRAR [F5]"));

                }
                player_allin_button.setForeground(Color.WHITE);
                player_allin_button.setBackground(new Color(51, 153, 255));
                player_allin_button.setEnabled(true);

                if (Game.TEST_MODE) {
                    player_allin_button.doClick();
                }
            }
        });

    }

    public void setSpectator(String msg) {
        if (!this.exit) {
            this.spectator = true;
            this.bote = 0f;

            if (crupier.getJugadoresActivos() + crupier.getTotalCalentando() < 2) {
                crupier.setJugadores_suficientes(false);
            }

            Helpers.GUIRun(new Runnable() {
                @Override
                public void run() {
                    setBorder(javax.swing.BorderFactory.createLineBorder(new Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                    player_blind.setVisible(false);
                    player_bet.setVisible(false);
                    utg_textfield.setVisible(false);
                    playingCard1.descargarCarta();
                    playingCard2.descargarCarta();
                    player_stack.setEnabled(false);
                    player_action.setText(msg != null ? msg : Translator.translate("ESPECTADOR"));
                    player_action.setEnabled(false);
                }
            });
        }
    }

    public void unsetSpectator() {
        this.spectator = false;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                setBorder(javax.swing.BorderFactory.createLineBorder(new Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                player_bet.setVisible(true);
                player_stack.setEnabled(true);
                player_action.setText(" ");
            }
        });
    }

    public void desactivar_boton_mostrar() {

        if (boton_mostrar) {
            boton_mostrar = false;

            Helpers.GUIRun(new Runnable() {
                public void run() {
                    player_allin_button.setText(" ");
                    player_allin_button.setEnabled(false);
                    player_allin_button.setBackground(Color.BLACK);
                    player_allin_button.setForeground(Color.WHITE);
                }
            });
        }
    }

    public void setTimeout(boolean val) {

        if (this.timeout_val != val) {

            this.timeout_val = val;

            Helpers.GUIRun(new Runnable() {
                public void run() {
                    timeout.setVisible(val);

                }
            });

            if (val) {
                Helpers.playWavResource("misc/network_error.wav");
            }
        }

    }

    public JLabel getAvatar() {
        return avatar;
    }

    public float getPagar() {
        return pagar;
    }

    public void setPagar(float pagar) {
        this.pagar = Helpers.clean1DFloat(pagar);
    }

    public float getBote() {
        return bote;
    }

    public synchronized boolean isExit() {
        return exit;
    }

    public synchronized void setExit(boolean exit) {
        this.exit = exit;
        this.spectator = false;

        if (crupier.getJugadoresActivos() + crupier.getTotalCalentando() < 2) {
            crupier.setJugadores_suficientes(false);
        }

        desactivarControles();

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                setBorder(javax.swing.BorderFactory.createLineBorder(new Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                timeout.setVisible(false);
                player_blind.setVisible(false);
                player_action.setVisible(false);
                player_bet.setVisible(false);
                utg_textfield.setVisible(false);
                playingCard1.descargarCarta();
                playingCard2.descargarCarta();

                player_action.setBackground(new Color(255, 102, 0));
                player_action.setForeground(Color.WHITE);
                player_action.setText(Translator.translate("ABANDONAS LA TIMBA"));
                player_action.setVisible(true);
                player_action.setEnabled(true);

            }
        });

    }

    @Override
    public boolean isSpectator() {
        return this.spectator;
    }

    public int getDecision() {
        return decision;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void setNickname(String nickname) {
        this.nickname = nickname;

        Helpers.GUIRun(new Runnable() {
            public void run() {
                player_name.setText(nickname);
            }
        });
    }

    public float getStack() {
        return stack;
    }

    public void setStack(float stack) {
        this.stack = Helpers.clean1DFloat(stack);

        Helpers.GUIRun(new Runnable() {
            public void run() {
                player_stack.setText(Helpers.float2String(stack));
            }
        });
    }

    public int getBuyin() {
        return buyin;
    }

    public float getBet() {
        return bet;
    }

    /**
     * Creates new form JugadorLocalView
     */
    public LocalPlayer() {

        Helpers.GUIRunAndWait(new Runnable() {
            public void run() {
                initComponents();

                action_button_colors.put(player_check_button, new Color[]{player_check_button.getBackground(), player_check_button.getForeground()});
                action_button_armed.put(player_check_button, false);

                action_button_colors.put(player_bet_button, new Color[]{player_bet_button.getBackground(), player_bet_button.getForeground()});
                action_button_armed.put(player_bet_button, false);

                action_button_colors.put(player_allin_button, new Color[]{player_allin_button.getBackground(), player_allin_button.getForeground()});
                action_button_armed.put(player_allin_button, false);

                action_button_colors.put(player_fold_button, new Color[]{player_fold_button.getBackground(), player_fold_button.getForeground()});
                action_button_armed.put(player_fold_button, false);

                bet_slider_text.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if ((e.getKeyChar() < '0' || e.getKeyChar() > '9') && e.getKeyChar() != '.') {
                            e.consume();
                        }
                    }
                });

                timeout.setVisible(false);

                decision = Player.NODEC;

                bet = 0f;

                player_check_button.setEnabled(false);

                bet_slider_text.setEnabled(false);

                bet_slider.setEnabled(false);

                player_bet_button.setEnabled(false);

                player_allin_button.setEnabled(false);

                player_fold_button.setEnabled(false);

                player_action.setText(" ");

                player_action.setEnabled(false);

                player_blind.setVisible(false);

                player_buyin.setText(String.valueOf(Game.BUYIN));

                utg_textfield.setVisible(false);

                player_bet.setBackground(Color.WHITE);

                player_bet.setForeground(Color.BLACK);

                player_bet.setText(" ---- ");

            }
        });
    }

    public JTextField getPlayer_action() {
        return player_action;
    }

    public void setPlayer_action(JTextField player_action) {
        this.player_action = player_action;
    }

    public JButton getPlayer_allin() {
        return player_allin_button;
    }

    public void setPlayer_allin(JButton player_allin) {
        this.player_allin_button = player_allin;
    }

    public JTextField getPlayer_bet() {
        return player_bet;
    }

    public void setPlayer_bet(JTextField player_bet) {
        this.player_bet = player_bet;
    }

    public JButton getPlayer_bet_button() {
        return player_bet_button;
    }

    public void setPlayer_bet_button(JButton player_bet_button) {
        this.player_bet_button = player_bet_button;
    }

    public JTextField getPlayer_buyin() {
        return player_buyin;
    }

    public void setPlayer_buyin(JTextField player_buyin) {
        this.player_buyin = player_buyin;
    }

    public JButton getPlayer_check() {
        return player_check_button;
    }

    public void setPlayer_check(JButton player_check) {
        this.player_check_button = player_check;
    }

    public JButton getPlayer_fold() {
        return player_fold_button;
    }

    public void setPlayer_fold(JButton player_fold) {
        this.player_fold_button = player_fold;
    }

    public JLabel getPlayer_name() {
        return player_name;
    }

    public void setPlayer_name(JLabel player_name) {
        this.player_name = player_name;
    }

    public JTextField getPlayer_stack() {
        return player_stack;
    }

    public void setPlayer_stack(JTextField player_stack) {
        this.player_stack = player_stack;
    }

    public Card getPlayingCard1() {
        return playingCard1;
    }

    public Card getPlayingCard2() {
        return playingCard2;
    }

    public void setBet(float new_bet) {

        float old_bet = this.bet;

        this.bet = Helpers.clean1DFloat(new_bet);

        if (Helpers.float1DSecureCompare(old_bet, this.bet) < 0) {
            this.bote += Helpers.clean1DFloat(this.bet - old_bet);
        }

        crupier.getBote().addPlayer(this);

        Helpers.GUIRun(new Runnable() {
            public void run() {

                if (Helpers.float1DSecureCompare(0f, bet) < 0) {
                    player_bet.setText(Helpers.float2String(bet));

                } else {
                    player_bet.setBackground(Color.WHITE);
                    player_bet.setForeground(Color.BLACK);
                    player_bet.setText(" ---- ");
                }

            }
        });

    }

    public void reComprar(int cantidad) {

        this.stack = cantidad;
        this.buyin += cantidad;
        Game.getInstance().getRegistro().print(this.nickname + Translator.translate(" RECOMPRA (") + String.valueOf(Game.BUYIN) + ")");
        Helpers.playWavResource("misc/cash_register.wav");

        Helpers.GUIRun(new Runnable() {
            public void run() {
                player_stack.setText(Helpers.float2String(stack));
                player_buyin.setText(String.valueOf(buyin));
                player_buyin.setBackground(Color.cyan);

            }
        });
    }

    public void esTuTurno() {

        turno = true;

        if (this.getDecision() == Player.NODEC) {
            Helpers.playWavResource("misc/yourturn.wav");

            call_required = Helpers.clean1DFloat(crupier.getApuesta_actual() - bet);

            min_raise = Helpers.float1DSecureCompare(0f, crupier.getUltimo_raise()) < 0 ? crupier.getUltimo_raise() : crupier.getCiega_grande();

            desarmarBotonesAccion();

            Helpers.GUIRun(new Runnable() {
                public void run() {

                    setBorder(javax.swing.BorderFactory.createLineBorder(Color.ORANGE, Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                    player_check_button.setEnabled(false);

                    player_check_button.setText(" ");

                    bet_slider_text.setEnabled(false);

                    bet_slider_text.setText(" ");

                    bet_slider.setEnabled(false);

                    player_bet_button.setEnabled(false);

                    player_bet_button.setText(" ");

                    player_allin_button.setText("ALL IN [F4]");

                    player_allin_button.setEnabled(true);

                    player_fold_button.setText("NO IR [F1]");

                    player_fold_button.setEnabled(true);

                    player_action.setBackground(Color.WHITE);

                    player_action.setForeground(Color.BLACK);

                    player_action.setEnabled(true);

                    player_action.setText("HABLAS TÚ");

                    player_bet.setBackground(Color.WHITE);

                    player_bet.setForeground(Color.BLACK);

                    //Comprobamos si podemos ver la apuesta actual
                    if (Helpers.float1DSecureCompare(call_required, stack) < 0) {

                        player_check_button.setEnabled(true);

                        if (Helpers.float1DSecureCompare(0f, call_required) == 0) {
                            player_check_button.setText("PASAR [F2]");
                        } else {
                            player_check_button.setText(Translator.translate("IR") + " (+" + Helpers.float2String(call_required) + ") [F2]");
                        }

                    }

                    if (crupier.puedenApostar(Game.getInstance().getJugadores()) > 1 && ((Helpers.float1DSecureCompare(0f, crupier.getApuesta_actual()) == 0 && Helpers.float1DSecureCompare(crupier.getCiega_grande(), stack) < 0)
                            || (Helpers.float1DSecureCompare(0f, crupier.getApuesta_actual()) < 0 && Helpers.float1DSecureCompare(call_required + min_raise, stack) < 0))) {

                        //Actualizamos el slider y el botón de apuestas
                        float slider_min;
                        float slider_max = Helpers.clean1DFloat(stack - call_required);
                        //slider_divisor = Helpers.clean1DFloat(1f/crupier.getCiega_pequeña());
                        slider_divisor = 1 / crupier.getCiega_pequeña();

                        if (Helpers.float1DSecureCompare(0f, crupier.getApuesta_actual()) == 0) {
                            player_bet_button.setEnabled(true);
                            player_bet_button.setText("APOSTAR [F3]");
                            slider_min = crupier.getCiega_grande();

                        } else {
                            player_bet_button.setEnabled(true);
                            player_bet_button.setText((crupier.getConta_raise() > 0 ? "RE" : "") + "SUBIR [F3]");
                            slider_min = min_raise;
                        }

                        if (slider_min < slider_max) {
                            BoundedRangeModel bRangeModel = new DefaultBoundedRangeModel();
                            bRangeModel.setMaximum(Math.round(slider_max * slider_divisor));
                            bRangeModel.setMinimum(Math.round(slider_min * slider_divisor));
                            bet_slider.setModel(bRangeModel);
                            bet_slider.setMinorTickSpacing(1);
                            bet_slider.setSnapToTicks(true);
                            bet_slider.setEnabled(true);
                            bet_slider_text.setEnabled(true);
                            bet_slider_text.setText(Helpers.float2String(slider_min));
                        } else {
                            player_bet_button.setEnabled(false);
                            player_bet_button.setText("");
                        }

                    }

                    if (crupier.puedenApostar(Game.getInstance().getJugadores()) == 1 && Helpers.float1DSecureCompare(call_required, stack) < 0) {
                        player_allin_button.setText(" ");
                        player_allin_button.setEnabled(false);
                    }

                    Game.getInstance().getBarra_tiempo().setMaximum(Game.TIEMPO_PENSAR);

                    Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                    Helpers.translateComponents(botonera, false);

                    Helpers.translateComponents(player_action, false);
                }
            });

            if (Game.TEST_MODE) {

                Helpers.threadRun(new Runnable() {
                    public void run() {

                        Helpers.pausar(Game.TEST_MODE_PAUSE);

                        ArrayList<JButton> botones = new ArrayList<>(Arrays.asList(new JButton[]{player_check_button, player_bet_button, player_allin_button, player_fold_button}));

                        Iterator<JButton> iterator = botones.iterator();

                        Helpers.GUIRun(new Runnable() {
                            public void run() {

                                while (iterator.hasNext()) {
                                    JButton boton = iterator.next();

                                    if (!boton.isEnabled()) {
                                        iterator.remove();
                                    }
                                }

                                int eleccion = Helpers.PRNG_GENERATOR.nextInt(botones.size());

                                botones.get(eleccion).doClick();
                            }
                        });

                    }
                });
            } else {

                //Tiempo máximo para pensar
                Helpers.threadRun(new Runnable() {
                    public void run() {

                        ActionListener listener = new ActionListener() {

                            int counter = Game.TIEMPO_PENSAR;

                            long t = crupier.getTurno();

                            public void actionPerformed(ActionEvent ae) {

                                if (!crupier.isFin_de_la_transmision() && !Game.getInstance().isTimba_pausada() && !WaitingRoom.isExit() && counter > 0 && auto_action.isRunning() && t == crupier.getTurno()) {

                                    counter--;

                                    Game.getInstance().getBarra_tiempo().setValue(counter);

                                    if (counter == 10) {
                                        Helpers.playWavResource("misc/hurryup.wav");
                                    }

                                    if (counter == 0 || !crupier.isJugadores_suficientes()) {

                                        Helpers.threadRun(new Runnable() {
                                            public void run() {

                                                if (counter == 0) {
                                                    Helpers.playWavResourceAndWait("misc/timeout.wav"); //Mientras dura la bocina aún estaríamos a tiempo de elegir
                                                }

                                                if (auto_action.isRunning() && t == crupier.getTurno() && getDecision() == Player.NODEC) {

                                                    Game.getInstance().checkPause();

                                                    if (auto_action.isRunning() && t == crupier.getTurno() && getDecision() == Player.NODEC) {

                                                        if (Helpers.float1DSecureCompare(0f, call_required) == 0) {

                                                            Helpers.GUIRun(new Runnable() {
                                                                public void run() {
                                                                    //Pasamos automáticamente 
                                                                    action_button_armed.put(player_check_button, true);
                                                                    player_check_button.doClick();
                                                                }
                                                            });

                                                        } else {

                                                            Helpers.GUIRun(new Runnable() {
                                                                public void run() {

                                                                    //Nos tiramos automáticamente
                                                                    action_button_armed.put(player_fold_button, true);
                                                                    player_fold_button.doClick();
                                                                }
                                                            });
                                                        }

                                                    }
                                                }
                                            }
                                        });

                                    }

                                }
                            }
                        };

                        if (auto_action != null) {
                            auto_action.stop();
                        }

                        auto_action = new Timer(1000, listener);

                        auto_action.start();

                    }
                });

            }

            if (Game.AUTO_ACTION_BUTTONS && pre_pulsado != Player.NODEC) {
                Helpers.GUIRun(new Runnable() {
                    public void run() {

                        if (pre_pulsado == Player.FOLD) {

                            player_fold_button.doClick();

                        } else if (pre_pulsado == Player.CHECK && (Helpers.float1DSecureCompare(0f, call_required) == 0 || (crupier.getFase() == Crupier.PREFLOP && Helpers.float1DSecureCompare(crupier.getApuesta_actual(), crupier.getCiega_grande()) == 0))) {

                            player_check_button.doClick();

                        } else {
                            desPrePulsarTodo();
                        }
                    }

                });
            }

        } else {

            finTurno();
        }

    }

    public void finTurno() {

        Helpers.stopWavResource("misc/hurryup.wav");

        Helpers.GUIRun(new Runnable() {
            public void run() {

                if (decision != Player.ALLIN) {
                    setBorder(javax.swing.BorderFactory.createLineBorder(new Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));
                }

                bet_slider_text.setText(" ");

                turno = false;

                synchronized (Game.getInstance().getCrupier().getLock_apuestas()) {
                    Game.getInstance().getCrupier().getLock_apuestas().notifyAll();
                }

                if (Game.AUTO_ACTION_BUTTONS && getDecision() != Player.ALLIN && getDecision() != Player.FOLD) {
                    activarPreBotones();
                }
            }
        });
    }

    public void desactivarControles() {

        Helpers.GUIRun(new Runnable() {
            public void run() {

                bet_slider_text.setText("");
                bet_slider_text.setEnabled(false);

                bet_slider.setEnabled(false);

                for (Map.Entry<JButton, Color[]> entry : action_button_colors.entrySet()) {

                    JButton b = entry.getKey();
                    b.setText(" ");
                    b.setEnabled(false);

                }

            }
        });

        desarmarBotonesAccion();
    }

    public void desPrePulsarTodo() {

        if (pre_pulsado != Player.NODEC) {

            desPrePulsarBoton(player_check_button);
            desPrePulsarBoton(player_fold_button);
        }
    }

    public void desPrePulsarBoton(JButton boton) {
        pre_pulsado = Player.NODEC;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                Color[] colores = action_button_colors.get(boton);

                boton.setBackground(colores[0]);

                boton.setForeground(colores[1]);

            }
        });

    }

    public void prePulsarBoton(JButton boton, int dec) {
        pre_pulsado = dec;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                boton.setBackground(Color.YELLOW);
                boton.setForeground(Color.BLACK);

            }
        });

    }

    public void desarmarBotonesAccion() {
        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<JButton, Color[]> entry : action_button_colors.entrySet()) {

                    JButton b = entry.getKey();

                    if (action_button_armed.get(b)) {

                        Color[] colores = entry.getValue();

                        action_button_armed.put(b, false);

                        b.setBackground(colores[0]);
                        b.setForeground(colores[1]);

                    }

                }
            }
        });
    }

    public void armarBoton(JButton boton) {

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                for (Map.Entry<JButton, Color[]> entry : action_button_colors.entrySet()) {

                    JButton b = entry.getKey();

                    Color[] colores = entry.getValue();

                    if (b == boton) {
                        action_button_armed.put(b, true);

                        b.setBackground(Color.BLUE);
                        b.setForeground(Color.WHITE);

                    } else {
                        action_button_armed.put(b, false);

                        b.setBackground(colores[0]);
                        b.setForeground(colores[1]);

                    }
                }

            }
        });

    }

    public void resetBetDecision() {
        this.decision = Player.NODEC;

        Helpers.GUIRun(new Runnable() {
            public void run() {

                player_action.setText(" ");

                player_action.setEnabled(false);
            }
        });
    }

    public void activarPreBotones() {

        if (!turno && decision != Player.FOLD && decision != Player.ALLIN && !crupier.isShow_time()) {

            Helpers.GUIRun(new Runnable() {
                public void run() {

                    player_check_button.setText(Translator.translate("(AUTO) PASAR [F2]"));
                    player_check_button.setEnabled(true);

                    player_fold_button.setText(Translator.translate("(AUTO) NO IR [F1]"));
                    player_fold_button.setEnabled(true);

                    if (pre_pulsado != Player.NODEC) {

                        if (pre_pulsado == Player.CHECK) {
                            prePulsarBoton(player_check_button, Player.CHECK);
                        } else if (pre_pulsado == Player.FOLD) {
                            prePulsarBoton(player_fold_button, Player.FOLD);
                        }
                    }

                }
            });
        }

    }

    public void desActivarPreBotones() {

        if (!turno) {

            this.desPrePulsarTodo();

            Helpers.GUIRun(new Runnable() {
                public void run() {

                    Color[] colores = action_button_colors.get(player_check_button);

                    player_check_button.setBackground(colores[0]);
                    player_check_button.setForeground(colores[1]);
                    player_check_button.setText(" ");
                    player_check_button.setEnabled(false);

                    colores = action_button_colors.get(player_fold_button);

                    player_fold_button.setBackground(colores[0]);
                    player_fold_button.setForeground(colores[1]);
                    player_fold_button.setText(" ");
                    player_fold_button.setEnabled(false);
                }
            });
        }
    }

    public void refreshPos() {

        this.bote = 0f;

        if (Helpers.float1DSecureCompare(0f, this.bet) < 0) {
            this.setStack(this.stack + this.bet);
        }

        this.bet = 0f;

        if (pos == crupier.getDealer_pos()) {
            this.setPosition(DEALER);
        } else if (pos == crupier.getBig_pos()) {
            this.setPosition(BIG_BLIND);
        } else if (pos == crupier.getSmall_pos()) {
            this.setPosition(SMALL_BLIND);
        }

        if (pos == crupier.getUtg_pos()) {
            this.setUTG();
        } else {
            this.disableUTG();
        }
    }

    @Override
    public void nuevaMano(int p) {

        desPrePulsarTodo();

        this.pos = p;

        if (this.crupier == null) {
            this.crupier = Game.getInstance().getCrupier();
        }

        this.decision = Player.NODEC;

        this.muestra = false;

        this.winner = false;

        this.loser = false;

        this.bote = 0f;

        this.bet = 0f;

        setStack(stack + pagar);

        pagar = 0f;

        if (Helpers.float1DSecureCompare(getStack(), 0f) == 0) {
            reComprar(Game.BUYIN);
        }

        Helpers.GUIRunAndWait(new Runnable() {
            public void run() {

                setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                desactivar_boton_mostrar();

                player_check_button.setText(" ");

                player_check_button.setEnabled(false);

                player_bet_button.setText(" ");

                player_bet_button.setEnabled(false);

                bet_slider_text.setText(" ");

                bet_slider_text.setEnabled(false);

                bet_slider.setEnabled(false);

                player_allin_button.setText(" ");

                player_allin_button.setEnabled(false);

                player_fold_button.setText(" ");

                player_fold_button.setEnabled(false);

                player_action.setText(" ");

                player_action.setEnabled(false);

                utg_textfield.setVisible(false);

                player_blind.setVisible(false);

                player_bet.setText(" ---- ");

                player_bet.setBackground(Color.WHITE);

                player_bet.setForeground(Color.BLACK);

                player_stack.setBackground(new Color(51, 255, 51));

                player_stack.setForeground(Color.BLACK);

            }
        });

        if (pos == crupier.getDealer_pos()) {
            this.setPosition(DEALER);
        } else if (pos == crupier.getBig_pos()) {
            this.setPosition(BIG_BLIND);
        } else if (pos == crupier.getSmall_pos()) {
            this.setPosition(SMALL_BLIND);
        }

        if (pos == crupier.getUtg_pos()) {
            this.setUTG();
        } else {
            this.disableUTG();
        }
    }

    public boolean isBoton_mostrar() {
        return boton_mostrar;
    }

    @Override
    public void disableUTG() {

        if (this.utg) {
            this.utg = false;

            Helpers.GUIRun(new Runnable() {
                @Override
                public void run() {
                    utg_textfield.setVisible(false);

                }
            });
        }
    }

    @Override
    public void zoom(float zoom_factor) {

        if (Helpers.float1DSecureCompare(0f, zoom_factor) < 0) {

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    player_action.setMinimumSize(new Dimension(Math.round(LocalPlayer.MIN_ACTION_WIDTH * zoom_factor), Math.round(LocalPlayer.MIN_ACTION_HEIGHT * zoom_factor)));
                    LineBorder border = (LineBorder) getBorder();
                    setBorder(javax.swing.BorderFactory.createLineBorder(border.getLineColor(), Math.round(Player.BORDER * zoom_factor)));
                }
            });

            playingCard1.zoom(zoom_factor);
            playingCard2.zoom(zoom_factor);
            Helpers.zoomFonts(this, zoom_factor);
        }

    }

    public void setPosition(int pos) {

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                player_bet.setBackground(Color.WHITE);
                player_bet.setForeground(Color.black);
            }
        });

        switch (pos) {
            case Player.DEALER:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        player_blind.setVisible(true);
                        player_blind.setBackground(Color.white);
                        player_blind.setForeground(Color.black);
                        player_blind.setText(POSITIONS_LABELS[2]);
                    }
                });

                if (crupier.getDealer_pos() == crupier.getSmall_pos()) {
                    if (Helpers.float1DSecureCompare(crupier.getCiega_pequeña(), stack) < 0) {
                        setBet(crupier.getCiega_pequeña());
                        setStack(stack - bet);

                    } else {

                        //Vamos ALLIN
                        setBet(stack);
                        setStack(0f);
                        setDecision(Player.ALLIN);
                    }
                } else {
                    setBet(0.0f);
                }

                break;
            case Player.BIG_BLIND:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        player_blind.setVisible(true);
                        player_blind.setBackground(Color.yellow);
                        player_blind.setForeground(Color.black);
                        player_blind.setText(POSITIONS_LABELS[1]);
                    }
                });

                if (Helpers.float1DSecureCompare(crupier.getCiega_grande(), stack) < 0) {
                    setBet(crupier.getCiega_grande());
                    setStack(stack - bet);

                } else {

                    //Vamos ALLIN
                    setBet(stack);
                    setStack(0f);
                    setDecision(Player.ALLIN);
                }

                break;
            case Player.SMALL_BLIND:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        player_blind.setVisible(true);
                        player_blind.setBackground(Color.BLUE);
                        player_blind.setForeground(Color.white);
                        player_blind.setText(POSITIONS_LABELS[0]);
                    }
                });

                if (Helpers.float1DSecureCompare(crupier.getCiega_pequeña(), stack) < 0) {
                    setBet(crupier.getCiega_pequeña());
                    setStack(stack - bet);

                } else {

                    //Vamos ALLIN
                    setBet(stack);
                    setStack(0f);
                    setDecision(Player.ALLIN);
                }

                break;
            default:
                setBet(0.0f);

                break;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        player_action = new javax.swing.JTextField();
        panel_cartas = new javax.swing.JPanel();
        playingCard2 = new com.tonikelope.coronapoker.Card();
        playingCard1 = new com.tonikelope.coronapoker.Card();
        indicadores_arriba = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        player_bet = new javax.swing.JTextField();
        avatar = new javax.swing.JLabel();
        player_stack = new javax.swing.JTextField();
        player_buyin = new javax.swing.JTextField();
        timeout = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        player_name = new javax.swing.JLabel();
        utg_textfield = new javax.swing.JLabel();
        player_blind = new javax.swing.JLabel();
        botonera = new javax.swing.JPanel();
        player_allin_button = new javax.swing.JButton();
        bet_slider = new javax.swing.JSlider();
        player_fold_button = new javax.swing.JButton();
        bet_slider_text = new javax.swing.JTextField();
        player_check_button = new javax.swing.JButton();
        player_bet_button = new javax.swing.JButton();

        setBackground(new java.awt.Color(0, 153, 51));
        setBorder(javax.swing.BorderFactory.createLineBorder(new Color(204, 204, 204), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL*Game.ZOOM_STEP))));
        setOpaque(false);

        player_action.setEditable(false);
        player_action.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        player_action.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        player_action.setText("NO VAS");
        player_action.setDoubleBuffered(true);
        player_action.setMinimumSize(new Dimension(Math.round(LocalPlayer.MIN_ACTION_WIDTH*(1f + Game.getZoom_level() * Game.getZOOM_STEP())), Math.round(LocalPlayer.MIN_ACTION_HEIGHT * (1f + Game.getZoom_level() * Game.getZOOM_STEP()))));

        panel_cartas.setOpaque(false);

        javax.swing.GroupLayout panel_cartasLayout = new javax.swing.GroupLayout(panel_cartas);
        panel_cartas.setLayout(panel_cartasLayout);
        panel_cartasLayout.setHorizontalGroup(
            panel_cartasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_cartasLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(playingCard1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(playingCard2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panel_cartasLayout.setVerticalGroup(
            panel_cartasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_cartasLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(panel_cartasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(playingCard1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(playingCard2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        indicadores_arriba.setOpaque(false);

        jPanel4.setOpaque(false);

        player_bet.setEditable(false);
        player_bet.setBackground(new java.awt.Color(255, 255, 255));
        player_bet.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        player_bet.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        player_bet.setText(" ---- ");
        player_bet.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        player_bet.setDoubleBuffered(true);

        avatar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/avatar_default.png"))); // NOI18N
        avatar.setDoubleBuffered(true);

        player_stack.setEditable(false);
        player_stack.setBackground(new java.awt.Color(51, 255, 51));
        player_stack.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        player_stack.setText("10000.0");
        player_stack.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        player_stack.setDoubleBuffered(true);

        player_buyin.setEditable(false);
        player_buyin.setBackground(new java.awt.Color(204, 204, 204));
        player_buyin.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        player_buyin.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        player_buyin.setText("20");
        player_buyin.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        player_buyin.setDoubleBuffered(true);

        timeout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/timeout.png"))); // NOI18N
        timeout.setDoubleBuffered(true);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(avatar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(player_stack, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(player_buyin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeout)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
                .addComponent(player_bet, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(avatar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(player_bet, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(timeout)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(player_stack, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(player_buyin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel5.setOpaque(false);

        player_name.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_name.setForeground(new java.awt.Color(255, 255, 255));
        player_name.setText("123456789012345");
        player_name.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        player_name.setDoubleBuffered(true);

        utg_textfield.setBackground(new java.awt.Color(255, 204, 204));
        utg_textfield.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        utg_textfield.setText("UTG");
        utg_textfield.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
        utg_textfield.setDoubleBuffered(true);
        utg_textfield.setOpaque(true);

        player_blind.setBackground(new java.awt.Color(51, 51, 255));
        player_blind.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_blind.setForeground(new java.awt.Color(255, 255, 255));
        player_blind.setText("CP");
        player_blind.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
        player_blind.setDoubleBuffered(true);
        player_blind.setOpaque(true);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(player_name)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(utg_textfield)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(player_blind))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(player_name)
                    .addComponent(utg_textfield)
                    .addComponent(player_blind))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout indicadores_arribaLayout = new javax.swing.GroupLayout(indicadores_arriba);
        indicadores_arriba.setLayout(indicadores_arribaLayout);
        indicadores_arribaLayout.setHorizontalGroup(
            indicadores_arribaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(indicadores_arribaLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(indicadores_arribaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        indicadores_arribaLayout.setVerticalGroup(
            indicadores_arribaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(indicadores_arribaLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        botonera.setOpaque(false);

        player_allin_button.setBackground(new java.awt.Color(0, 0, 0));
        player_allin_button.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_allin_button.setForeground(new java.awt.Color(255, 255, 255));
        player_allin_button.setText("ALL IN [F4]");
        player_allin_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        player_allin_button.setDoubleBuffered(true);
        player_allin_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                player_allin_buttonActionPerformed(evt);
            }
        });

        bet_slider.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        bet_slider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        bet_slider.setDoubleBuffered(true);
        bet_slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bet_sliderStateChanged(evt);
            }
        });

        player_fold_button.setBackground(new java.awt.Color(255, 0, 0));
        player_fold_button.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_fold_button.setForeground(new java.awt.Color(255, 255, 255));
        player_fold_button.setText("NO IR [F1]");
        player_fold_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        player_fold_button.setDoubleBuffered(true);
        player_fold_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                player_fold_buttonActionPerformed(evt);
            }
        });

        bet_slider_text.setEditable(false);
        bet_slider_text.setBackground(java.awt.Color.orange);
        bet_slider_text.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        bet_slider_text.setForeground(new java.awt.Color(0, 0, 0));
        bet_slider_text.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        bet_slider_text.setDoubleBuffered(true);

        player_check_button.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_check_button.setText("PASAR [F2]");
        player_check_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        player_check_button.setDoubleBuffered(true);
        player_check_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                player_check_buttonActionPerformed(evt);
            }
        });

        player_bet_button.setBackground(java.awt.Color.orange);
        player_bet_button.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        player_bet_button.setText("APOSTAR [F3]");
        player_bet_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        player_bet_button.setDoubleBuffered(true);
        player_bet_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                player_bet_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout botoneraLayout = new javax.swing.GroupLayout(botonera);
        botonera.setLayout(botoneraLayout);
        botoneraLayout.setHorizontalGroup(
            botoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botoneraLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(botoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(player_bet_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(player_allin_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(player_check_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bet_slider, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(bet_slider_text)
                    .addComponent(player_fold_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        botoneraLayout.setVerticalGroup(
            botoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(botoneraLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(player_check_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bet_slider_text)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bet_slider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(player_bet_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(player_allin_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(player_fold_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(player_action, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(indicadores_arriba, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel_cartas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(botonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(indicadores_arriba, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(panel_cartas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(botonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(player_action, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void player_fold_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_player_fold_buttonActionPerformed
        // TODO add your handling code here:

        if (!turno) {

            synchronized (pre_pulsar_lock) {

                if (pre_pulsado == Player.FOLD) {

                    Helpers.playWavResource("misc/auto_button_off.wav");

                    this.desPrePulsarBoton(player_fold_button);

                } else {
                    Helpers.playWavResource("misc/auto_button_on.wav");

                    this.desPrePulsarTodo();

                    this.prePulsarBoton(player_fold_button, Player.FOLD);
                }
            }

        } else if (!Game.getInstance().isTimba_pausada() && !WaitingRoom.isExit() && getDecision() == Player.NODEC && player_fold_button.isEnabled()) {

            if (pre_pulsado == Player.FOLD || !Game.CONFIRM_ACTIONS || Helpers.float1DSecureCompare(0f, bote) == 0 || this.action_button_armed.get(player_fold_button) || click_recuperacion) {

                Helpers.playWavResource("misc/fold.wav");

                desactivarControles();

                Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                if (auto_action != null) {
                    auto_action.stop();

                }

                Helpers.threadRun(new Runnable() {
                    public void run() {

                        crupier.soundFold();

                        setDecision(Player.FOLD);

                        playingCard1.desenfocar();
                        playingCard2.desenfocar();

                        finTurno();
                    }
                });

            } else {

                this.armarBoton(player_fold_button);
            }

        }

    }//GEN-LAST:event_player_fold_buttonActionPerformed

    private void player_allin_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_player_allin_buttonActionPerformed
        // TODO add your handling code here:

        if (!Game.getInstance().isTimba_pausada() && !WaitingRoom.isExit()) {

            if (player_allin_button.isEnabled()) {

                if (boton_mostrar && crupier.isShow_time()) {

                    this.muestra = true;

                    if (decision == Player.FOLD) {
                        updateParguela_counter();
                    }

                    desactivarControles();

                    desactivar_boton_mostrar();

                    Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                    if (auto_action != null) {
                        auto_action.stop();

                    }

                    Helpers.threadRun(new Runnable() {
                        public void run() {

                            if (crupier.isShow_time()) {

                                Helpers.threadRun(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (crupier.isShow_time()) {

                                            if (Game.getInstance().isPartida_local()) {
                                                crupier.showAndBroadcastPlayerCards(nickname);
                                            } else {
                                                crupier.sendCommandToServer("SHOWMYCARDS");
                                                crupier.setTiempo_pausa(Game.PAUSA_ENTRE_MANOS);
                                            }
                                        }
                                    }
                                });

                                ArrayList<Card> cartas = new ArrayList<>();

                                cartas.add(getPlayingCard1());
                                cartas.add(getPlayingCard2());

                                String lascartas = Card.collection2String(cartas);

                                for (Card carta_comun : Game.getInstance().getCartas_comunes()) {

                                    if (!carta_comun.isTapada()) {
                                        cartas.add(carta_comun);
                                    }
                                }

                                Hand jugada = new Hand(cartas);

                                player_action.setForeground(Color.WHITE);

                                player_action.setBackground(new Color(51, 153, 255));

                                player_action.setText(Translator.translate(" MUESTRAS (") + jugada.getName() + ")");

                                if (Game.SONIDOS_CHORRA && decision == Player.FOLD) {

                                    Helpers.playWavResource("misc/showyourcards.wav");

                                }

                                if (!crupier.getPerdedores().containsKey(this)) {
                                    Game.getInstance().getRegistro().print(nickname + Translator.translate(" MUESTRA (") + lascartas + ") -> " + jugada);
                                }

                                Helpers.translateComponents(botonera, false);

                                Helpers.translateComponents(player_action, false);

                            }

                        }
                    });

                } else if (getDecision() == Player.NODEC) {

                    if (Game.TEST_MODE || this.action_button_armed.get(player_allin_button) || click_recuperacion) {

                        Helpers.playWavResource("misc/allin.wav");

                        desactivarControles();

                        Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                        if (auto_action != null) {
                            auto_action.stop();
                        }

                        crupier.setPlaying_cinematic(true);

                        Helpers.threadRun(new Runnable() {

                            public void run() {

                                if (!crupier.localCinematicAllin()) {
                                    crupier.soundAllin();
                                }
                            }
                        });

                        setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLACK, Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));

                        Helpers.threadRun(new Runnable() {
                            public void run() {

                                setBet(stack + bet);

                                setStack(0f);

                                setDecision(Player.ALLIN);

                                finTurno();
                            }
                        });
                    } else {

                        this.armarBoton(player_allin_button);
                    }

                }

            }
        }

    }//GEN-LAST:event_player_allin_buttonActionPerformed

    private void player_check_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_player_check_buttonActionPerformed
        // TODO add your handling code here:
        if (!turno) {

            synchronized (pre_pulsar_lock) {

                if (pre_pulsado == Player.CHECK) {

                    Helpers.playWavResource("misc/auto_button_off.wav");

                    this.desPrePulsarBoton(player_check_button);

                } else {

                    Helpers.playWavResource("misc/auto_button_on.wav");

                    this.desPrePulsarTodo();

                    this.prePulsarBoton(player_check_button, Player.CHECK);
                }
            }

        } else if (!Game.getInstance().isTimba_pausada() && !WaitingRoom.isExit() && getDecision() == Player.NODEC && player_check_button.isEnabled()) {

            if (pre_pulsado == Player.CHECK || !Game.CONFIRM_ACTIONS || this.action_button_armed.get(player_check_button) || click_recuperacion) {

                if (Helpers.float1DSecureCompare(this.stack - (crupier.getApuesta_actual() - this.bet), 0f) == 0) {
                    player_allin_buttonActionPerformed(null);
                } else {

                    Helpers.playWavResource("misc/check.wav");

                    desactivarControles();

                    Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                    if (auto_action != null) {
                        auto_action.stop();

                    }

                    Helpers.threadRun(new Runnable() {
                        public void run() {

                            setStack(stack - (crupier.getApuesta_actual() - bet));

                            setBet(crupier.getApuesta_actual());

                            setDecision(Player.CHECK);

                            finTurno();
                        }
                    });
                }
            } else {

                this.armarBoton(player_check_button);
            }
        }

    }//GEN-LAST:event_player_check_buttonActionPerformed

    private void player_bet_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_player_bet_buttonActionPerformed
        // TODO add your handling code here:

        if (!Game.getInstance().isTimba_pausada() && !WaitingRoom.isExit() && getDecision() == Player.NODEC && player_bet_button.isEnabled()) {

            if (Helpers.float1DSecureCompare(stack, Float.valueOf(bet_slider_text.getText()) + call_required) == 0) {

                player_allin_buttonActionPerformed(null);

            } else {

                if (!Game.CONFIRM_ACTIONS || this.action_button_armed.get(player_bet_button) || click_recuperacion) {

                    float bet_slider_val = Helpers.clean1DFloat(Float.valueOf(bet_slider_text.getText()));

                    Helpers.playWavResource("misc/bet.wav");

                    desactivarControles();

                    Game.getInstance().getBarra_tiempo().setValue(Game.TIEMPO_PENSAR);

                    if (auto_action != null) {
                        auto_action.stop();
                    }

                    Helpers.threadRun(new Runnable() {
                        public void run() {

                            if (apuesta_recuperada == null) {

                                setStack(stack - (bet_slider_val + call_required));

                                setBet(bet_slider_val + bet + call_required);
                            } else {
                                setStack(stack - (apuesta_recuperada - bet));

                                setBet(apuesta_recuperada);

                                apuesta_recuperada = null;
                            }

                            setDecision(Player.BET);

                            if (!crupier.isSincronizando_mano() && Game.SONIDOS_CHORRA && crupier.getConta_raise() > 0 && Helpers.float1DSecureCompare(crupier.getApuesta_actual(), bet) < 0 && Helpers.float1DSecureCompare(0f, crupier.getApuesta_actual()) < 0) {
                                Helpers.playWavResource("misc/raise.wav");
                            }

                            finTurno();

                        }
                    });
                } else {
                    this.armarBoton(player_bet_button);
                }
            }

        }

    }//GEN-LAST:event_player_bet_buttonActionPerformed

    private void bet_sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bet_sliderStateChanged
        // TODO add your handling code here:
        bet_slider_text.setText(Helpers.float2String(bet_slider.getValue() / slider_divisor));
    }//GEN-LAST:event_bet_sliderStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel avatar;
    private javax.swing.JSlider bet_slider;
    private javax.swing.JTextField bet_slider_text;
    private javax.swing.JPanel botonera;
    private javax.swing.JPanel indicadores_arriba;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel panel_cartas;
    private javax.swing.JTextField player_action;
    private javax.swing.JButton player_allin_button;
    private javax.swing.JTextField player_bet;
    private javax.swing.JButton player_bet_button;
    private javax.swing.JLabel player_blind;
    private javax.swing.JTextField player_buyin;
    private javax.swing.JButton player_check_button;
    private javax.swing.JButton player_fold_button;
    private javax.swing.JLabel player_name;
    private javax.swing.JTextField player_stack;
    private com.tonikelope.coronapoker.Card playingCard1;
    private com.tonikelope.coronapoker.Card playingCard2;
    private javax.swing.JLabel timeout;
    private javax.swing.JLabel utg_textfield;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setWinner(String msg) {
        this.winner = true;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                setBorder(javax.swing.BorderFactory.createLineBorder(Color.GREEN, Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));
                player_action.setEnabled(true);
                player_action.setBackground(Color.GREEN);
                player_action.setForeground(Color.BLACK);
                player_bet.setBackground(Color.GREEN);
                player_bet.setForeground(Color.BLACK);
                player_action.setText(msg);

            }
        });
    }

    @Override
    public void setLoser(String msg) {
        this.loser = true;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                setBorder(javax.swing.BorderFactory.createLineBorder(Color.RED, Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));
                player_action.setEnabled(true);
                player_action.setBackground(Color.RED);
                player_action.setForeground(Color.WHITE);
                player_bet.setBackground(Color.RED);
                player_bet.setForeground(Color.WHITE);
                player_action.setText(msg);

                playingCard1.desenfocar();
                playingCard2.desenfocar();

                if (Helpers.float1DSecureCompare(stack, 0f) == 0) {
                    player_stack.setBackground(Color.RED);
                    player_stack.setForeground(Color.WHITE);
                }

            }
        });

    }

    @Override
    public void setBoteSecundario(String msg) {

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                player_action.setText(player_action.getText() + " " + msg);
            }
        });
    }

    @Override
    public void pagar(float pasta) {

        this.pagar += pasta;

    }

    @Override
    public void disablePlayer(boolean visible) {

        if (auto_action != null) {
            auto_action.stop();
        }

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                if (visible) {
                    setBorder(javax.swing.BorderFactory.createLineBorder(new Color(0, 0, 0, 0), Math.round(Player.BORDER * (1f + Game.ZOOM_LEVEL * Game.ZOOM_STEP))));
                    playingCard1.descargarCarta();
                    playingCard2.descargarCarta();
                    indicadores_arriba.setVisible(false);
                    player_action.setVisible(false);
                    botonera.setVisible(false);
                } else {
                    setVisible(false);
                }
            }
        });
    }

    public void setUTG() {

        this.utg = true;

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {
                utg_textfield.setVisible(true);
                utg_textfield.setText("UTG");
                utg_textfield.setBackground(Color.PINK);
                utg_textfield.setForeground(Color.BLACK);

            }
        });
    }

    public void setDecision(int dec) {

        this.decision = dec;

        switch (dec) {
            case Player.CHECK:

                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        if (Helpers.float1DSecureCompare(0f, call_required) < 0) {
                            player_action.setText(ACTIONS_LABELS[dec - 1][1]);
                        } else {
                            player_action.setText(ACTIONS_LABELS[dec - 1][0]);
                        }
                    }
                });

                break;
            case Player.BET:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        if (Helpers.float1DSecureCompare(crupier.getApuesta_actual(), bet) < 0 && Helpers.float1DSecureCompare(0f, crupier.getApuesta_actual()) < 0) {
                            player_action.setText((crupier.getConta_raise() > 0 ? "RE" : "") + ACTIONS_LABELS[dec - 1][1] + " (+" + Helpers.float2String(bet - crupier.getApuesta_actual()) + ")");
                        } else {
                            player_action.setText(ACTIONS_LABELS[dec - 1][0] + " " + Helpers.float2String(bet));
                        }

                    }
                });
                break;
            case Player.ALLIN:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        player_action.setText(ACTIONS_LABELS[dec - 1][0] + " (" + Helpers.float2String(bet + getStack()) + ")");
                    }
                });
                break;
            default:
                Helpers.GUIRun(new Runnable() {
                    @Override
                    public void run() {
                        player_action.setText(ACTIONS_LABELS[dec - 1][0]);
                    }
                });
                break;
        }

        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                player_action.setBackground(ACTIONS_COLORS[dec - 1][0]);

                player_bet.setBackground(ACTIONS_COLORS[dec - 1][0]);

                player_action.setForeground(ACTIONS_COLORS[dec - 1][1]);

                player_bet.setForeground(ACTIONS_COLORS[dec - 1][1]);

                player_action.setEnabled(true);
            }
        });
    }

    @Override
    public String getLastActionString() {

        String action = nickname + " ";

        switch (this.getDecision()) {
            case Player.FOLD:
                action += player_action.getText() + " (" + Helpers.float2String(this.bote) + ")";
                break;
            case Player.CHECK:
                action += player_action.getText() + " (" + Helpers.float2String(this.bote) + ")";
                break;
            case Player.BET:
                action += player_action.getText() + " (" + Helpers.float2String(this.bote) + ")";
                break;
            case Player.ALLIN:
                action += player_action.getText();
                break;
            default:
                break;
        }

        return action;
    }

    @Override
    public void enableMantenimiento() {
        Helpers.GUIRun(new Runnable() {
            @Override
            public void run() {

                player_stack.setEditable(true);

                player_buyin.setEditable(true);

            }
        });
    }

    @Override
    public void disableMantenimiento() {
        Helpers.GUIRunAndWait(new Runnable() {
            @Override
            public void run() {

                player_stack.setEditable(false);

                player_buyin.setEditable(false);

            }
        });

        this.stack = Helpers.clean1DFloat(Float.valueOf(this.player_stack.getText().trim()));

        this.buyin = Integer.valueOf(this.player_buyin.getText().trim());
    }

    public void setBuyin(int buyin) {
        this.buyin = buyin;

        Helpers.GUIRun(new Runnable() {
            public void run() {
                player_buyin.setText(String.valueOf(buyin));

                if (buyin > Game.BUYIN) {
                    player_buyin.setBackground(Color.cyan);
                }
            }
        });

    }

    @Override
    public void setServer() {

        Helpers.GUIRun(new Runnable() {
            public void run() {
                getAvatar().setBorder(javax.swing.BorderFactory.createLineBorder(Color.YELLOW, 2));
            }
        });

    }

    @Override
    public void showCards(String jugada) {
        Helpers.GUIRun(new Runnable() {
            public void run() {
                player_action.setBackground(new Color(51, 153, 255));
                player_action.setForeground(Color.WHITE);
                player_action.setText("MUESTRA (" + jugada + ")");
            }
        });
    }

    @Override
    public void resetBote() {
        this.bote = 0f;
    }

}
