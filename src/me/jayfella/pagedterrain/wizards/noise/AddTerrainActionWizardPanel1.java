/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.jayfella.pagedterrain.wizards.noise;

import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;

public class AddTerrainActionWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor>
{
    private Node selectedNode;

    public AddTerrainActionWizardPanel1(Node node)
    {
        selectedNode = node;
    }

    private AddTerrainActionVisualPanel1 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public AddTerrainActionVisualPanel1 getComponent()
    {
        if (component == null)
        {
            component = new AddTerrainActionVisualPanel1(selectedNode);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp()
    {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid()
    {
        // If it is always OK to press Next or Finish, then:
        return true;
        // If it depends on some condition (form filled out...) and
        // this condition changes (last form field filled in...) then
        // use ChangeSupport to implement add/removeChangeListener below.
        // WizardDescriptor.ERROR/WARNING/INFORMATION_MESSAGE will also be useful.
    }

    @Override
    public void addChangeListener(ChangeListener l)
    {
    }

    @Override
    public void removeChangeListener(ChangeListener l)
    {
    }

    @Override
    public void readSettings(WizardDescriptor wiz)
    {
        // use wiz.getProperty to retrieve previous panel state
    }

    @Override
    public void storeSettings(WizardDescriptor wiz)
    {
        // General Settings
        wiz.putProperty("blockSize", this.getComponent().getBlockSize());
        wiz.putProperty("patchSize", this.getComponent().getPatchSize());

        wiz.putProperty("scaleX", this.getComponent().getScaleX());
        wiz.putProperty("scaleY", this.getComponent().getScaleY());
        wiz.putProperty("scaleZ", this.getComponent().getScaleZ());

        wiz.putProperty("vd_north", this.getComponent().getViewDistanceNorth());
        wiz.putProperty("vd_east", this.getComponent().getViewDistanceEast());
        wiz.putProperty("vd_south", this.getComponent().getViewDistanceSouth());
        wiz.putProperty("vd_west", this.getComponent().getViewDistanceWest());

        wiz.putProperty("defaultMaterial", this.getComponent().getSelectedMaterial());

        // Noise Data
        wiz.putProperty("noise_roughness", this.getComponent().getNoiseRoughness());
        wiz.putProperty("noise_frequency", this.getComponent().getNoiseFrequency());
        wiz.putProperty("noise_amplitude", this.getComponent().getNoiseAmplitude());
        wiz.putProperty("noise_lacunarity", this.getComponent().getNoiseLacunarity());
        wiz.putProperty("noise_octaves", this.getComponent().getNoiseOctaves());
        wiz.putProperty("noise_scale", this.getComponent().getNoiseScale());
        wiz.putProperty("noise_perturbMagnitude", this.getComponent().getNoisePerturbMagnitude());
        wiz.putProperty("noise_erosionRadius", this.getComponent().getNoiseErosionRadius());
        wiz.putProperty("noise_erosionTalus", this.getComponent().getNoiseErosionTalus());
        wiz.putProperty("noise_smoothRadius", this.getComponent().getNoiseSmoothRadius());
        wiz.putProperty("noise_smoothEffect", this.getComponent().getNoiseSmoothEffect());
        wiz.putProperty("noise_iterations", this.getComponent().getNoiseIterations());
    }
}
