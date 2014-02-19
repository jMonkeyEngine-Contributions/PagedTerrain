package me.jayfella.pagedterrain.wizards.noise;

import me.jayfella.pagedterrain.core.GridBasedTerrain;
import com.jme3.app.SimpleApplication;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.FakeApplication;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractNewControlWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewControlAction;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import java.awt.Component;
import java.awt.Dialog;
import java.text.MessageFormat;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;

@org.openide.util.lookup.ServiceProvider(service = NewControlAction.class)
public class AddTerrainAction extends AbstractNewControlWizardAction
{

    private WizardDescriptor.Panel[] panels;
    private Node selectedNode;

    public AddTerrainAction()
    {
        name = "Paged Terrain...";
    }

    @Override
    protected Object showWizard(Node node)
    {
        selectedNode = node;

        WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Paged Terrain Wizard");
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();

        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;

        if (!cancelled)
        {
            return wizardDescriptor;
        }

        return null;
    }

    @Override
    protected Control doCreateControl(Spatial sptl, Object properties)
    {
        if (properties != null)
        {
            return generatePagedTerrain((com.jme3.scene.Node)sptl, (WizardDescriptor) properties);
        }

        return null;
    }

    private Control generatePagedTerrain(com.jme3.scene.Node parent, WizardDescriptor wiz)
    {
        int blockSize = Integer.parseInt((String)wiz.getProperty("blockSize")) + 1;
        int patchSize = Integer.parseInt((String)wiz.getProperty("patchSize")) + 1;

        int scaleX = Integer.parseInt((String)wiz.getProperty("scaleX"));
        int scaleY = Integer.parseInt((String)wiz.getProperty("scaleY"));
        int scaleZ = Integer.parseInt((String)wiz.getProperty("scaleZ"));

        int vd_north = Integer.parseInt((String)wiz.getProperty("vd_north"));
        int vd_east = Integer.parseInt((String)wiz.getProperty("vd_east"));
        int vd_south = Integer.parseInt((String)wiz.getProperty("vd_south"));
        int vd_west = Integer.parseInt((String)wiz.getProperty("vd_west"));

        final ProjectAssetManager manager = selectedNode.getLookup().lookup(ProjectAssetManager.class);
        final SimpleApplication app = selectedNode.getLookup().lookup(FakeApplication.class);

        String defaultMaterial = (String)wiz.getProperty("defaultMaterial");

        Vector3f scale = new Vector3f(scaleX, scaleY, scaleZ);

        GridBasedTerrain terrain = new GridBasedTerrain(manager, parent, blockSize, patchSize, scale, vd_north, vd_east, vd_south, vd_west, defaultMaterial);

        float noise_roughness = Float.parseFloat(wiz.getProperty("noise_roughness").toString());
        float noise_frequency = Float.parseFloat(wiz.getProperty("noise_frequency").toString());
        float noise_amplitude = Float.parseFloat(wiz.getProperty("noise_amplitude").toString());
        float noise_lacunarity = Float.parseFloat(wiz.getProperty("noise_lacunarity").toString());
        float noise_octaves = Float.parseFloat(wiz.getProperty("noise_octaves").toString());
        float noise_scale = Float.parseFloat(wiz.getProperty("noise_scale").toString());
        float noise_perturbMagnitude = Float.parseFloat(wiz.getProperty("noise_perturbMagnitude").toString());
        int noise_erosionRadius = Integer.parseInt(wiz.getProperty("noise_erosionRadius").toString());
        float noise_erosionTalus = Float.parseFloat(wiz.getProperty("noise_erosionTalus").toString());
        int noise_smoothRadius = Integer.parseInt(wiz.getProperty("noise_smoothRadius").toString());
        float noise_smoothEffect = Float.parseFloat(wiz.getProperty("noise_smoothEffect").toString());
        int noise_iterations = Integer.parseInt(wiz.getProperty("noise_iterations").toString());

        FractalSum base = new FractalSum();
        base.setRoughness(noise_roughness);
        base.setFrequency(noise_frequency);
        base.setAmplitude(noise_amplitude);
        base.setLacunarity(noise_lacunarity);
        base.setOctaves(noise_octaves);
        base.setScale(noise_scale);
        base.addModulator(new NoiseModulator()
            {
                @Override public float value(float... in)
                {
                    return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
                }
            });

        FilteredBasis ground = new FilteredBasis(base);
        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(noise_perturbMagnitude);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(noise_erosionRadius);
        therm.setTalus(noise_erosionTalus);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(noise_smoothRadius);
        smooth.setEffect(noise_smoothEffect);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(noise_iterations);

        ground.addPreFilter(iterate);

        terrain.setFilteredBasis(ground);

        // parent.addControl(terrain);
        return terrain;
    }

    private WizardDescriptor.Panel[] getPanels()
    {
        if (panels == null)
        {
            panels = new WizardDescriptor.Panel[]
            {
                new AddTerrainActionWizardPanel1(selectedNode),
                // new SkyboxWizardPanel2()
            };

            String[] steps = new String[panels.length];

            for (int i = 0; i < panels.length; i++)
            {
                Component c = panels[i].getComponent();

                steps[i] = c.getName();

                if (c instanceof JComponent)
                {
                    JComponent jc = (JComponent)c;

                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.FALSE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.FALSE);
                    // Turn on numbering of all steps
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.FALSE);

                }

            }

        }

        return panels;
    }


}
