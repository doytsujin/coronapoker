/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonikelope.coronapoker;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

/**
 *
 * @author tonikelope
 */
public class Reconnect2ServerDialog extends javax.swing.JDialog {

    private boolean reconectar = false;

    public void reset() {

        Helpers.GUIRun(new Runnable() {
            public void run() {
                getIp_port().setEnabled(true);
                getYes().setText("RECONECTAR");
                getYes().setEnabled(true);
                getStatus().setEnabled(true);
                getStatus2().setEnabled(true);
                getBarra().setVisible(false);
                pack();
            }
        });
    }

    public boolean isReconectar() {
        return reconectar;
    }

    public void setReconectar(boolean reconectar) {
        this.reconectar = reconectar;
    }

    public JTextField getIp_port() {
        return ip_port;
    }

    public JButton getYes() {
        return yes;
    }

    public JProgressBar getBarra() {
        return barra;
    }

    public JLabel getStatus() {
        return status;
    }

    public JLabel getStatus2() {
        return status2;
    }

    /**
     * Creates new form Reconnect2ServerDialog
     */
    public Reconnect2ServerDialog(java.awt.Frame parent, boolean modal, String ip_port) {
        super(parent, modal);
        initComponents();
        this.ip_port.setText(ip_port);
        this.barra.setVisible(false);
        this.barra.setIndeterminate(true);
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        status = new javax.swing.JLabel();
        ip_port = new javax.swing.JTextField();
        yes = new javax.swing.JButton();
        barra = new javax.swing.JProgressBar();
        status2 = new javax.swing.JLabel();

        setUndecorated(true);
        setResizable(false);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        status.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        status.setText("SE PERDIÓ LA CONEXIÓN CON EL SERVIDOR");
        status.setDoubleBuffered(true);

        ip_port.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        ip_port.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ip_port.setText("tonikelope.noestasinvitado.com:23456");
        ip_port.setDoubleBuffered(true);

        yes.setBackground(new java.awt.Color(0, 204, 51));
        yes.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        yes.setForeground(new java.awt.Color(255, 255, 255));
        yes.setText("RECONECTAR");
        yes.setDoubleBuffered(true);
        yes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yesActionPerformed(evt);
            }
        });

        barra.setDoubleBuffered(true);

        status2.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        status2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        status2.setText("(Comprueba si la dirección o el puerto han cambiado antes de reconectar)");
        status2.setDoubleBuffered(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ip_port)
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(yes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(barra, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ip_port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(status)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(status2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(barra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(yes)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void yesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yesActionPerformed
        // TODO add your handling code here:
        this.ip_port.setEnabled(false);
        this.status.setEnabled(false);
        this.status2.setEnabled(false);
        this.yes.setEnabled(false);
        this.yes.setText("Reconectando...");
        this.barra.setVisible(true);
        this.reconectar = true;
        pack();

        Helpers.threadRun(new Runnable() {
            public void run() {
                synchronized (WaitingRoom.getInstance().getLock_reconnect()) {
                    WaitingRoom.getInstance().getLock_reconnect().notifyAll();
                }
            }
        });
    }//GEN-LAST:event_yesActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        if (WaitingRoom.isPartida_empezada()) {
            Game.getInstance().getExit_menu().doClick();
        }
    }//GEN-LAST:event_formWindowClosing

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
        // TODO add your handling code here:
        Helpers.playWavResource("misc/warning.wav");
    }//GEN-LAST:event_formComponentShown

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar barra;
    private javax.swing.JTextField ip_port;
    private javax.swing.JLabel status;
    private javax.swing.JLabel status2;
    private javax.swing.JButton yes;
    // End of variables declaration//GEN-END:variables
}
