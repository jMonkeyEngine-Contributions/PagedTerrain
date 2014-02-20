/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.jayfella.pagedterrain.wizards.noise;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.openide.nodes.Node;

public final class AddTerrainActionVisualPanel1 extends JPanel
{
    private FilteredBasis noiseGen;

    public AddTerrainActionVisualPanel1(Node selectedNode)
    {
        initComponents();

        populateMaterialComboBox(selectedNode);
        createTableListener();
    }

    private void populateMaterialComboBox(Node selectedNode)
    {
        final ProjectAssetManager manager = selectedNode.getLookup().lookup(ProjectAssetManager.class);

        for (String string : manager.getMaterials())
            jComboBox1.addItem(string);

        jComboBox1.validate();
    }

    private void createTableListener()
    {
        TableListener tl = new TableListener();
        jTable2.getModel().addTableModelListener(tl);
    }


    @Override
    public String getName()
    {
        return "Add Paged Terrain Wizard";
    }

    // Terrain Size
    public String getBlockSize() { return jTextField1.getText(); }
    public String getPatchSize() { return jTextField2.getText(); }

    // Terrain Scale
    public String getScaleX() { return jTextField3.getText(); }
    public String getScaleY() { return jTextField4.getText(); }
    public String getScaleZ() { return jTextField5.getText(); }

    // View Distance
    public String getViewDistanceNorth() { return jTextField7.getText(); }
    public String getViewDistanceEast() { return jTextField6.getText(); }
    public String getViewDistanceSouth() { return jTextField8.getText(); }
    public String getViewDistanceWest() { return jTextField9.getText(); }

    // Material
    public String getSelectedMaterial() { return jComboBox1.getSelectedItem().toString(); }

    // Noise
    private float noise_roughness;
    private float noise_frequency;
    private float noise_amplitude;
    private float noise_lacunarity;
    private float noise_octaves;
    private float noise_scale;
    private float noise_perturbMagnitude;
    private int noise_erosionRadius;
    private float noise_erosionTalus;
    private int noise_smoothRadius;
    private float noise_smoothEffect;
    private int noise_iterations;

    public float getNoiseRoughness() { return noise_roughness; }
    public float getNoiseFrequency() { return noise_frequency; }
    public float getNoiseAmplitude() { return noise_amplitude; }
    public float getNoiseLacunarity() { return noise_lacunarity; }
    public float getNoiseOctaves() { return noise_octaves; }
    public float getNoiseScale() { return noise_scale; }
    public float getNoisePerturbMagnitude() { return noise_perturbMagnitude; }
    public int getNoiseErosionRadius() { return noise_erosionRadius; }
    public float getNoiseErosionTalus() { return noise_erosionTalus; }
    public int getNoiseSmoothRadius() { return noise_smoothRadius; }
    public float getNoiseSmoothEffect() { return noise_smoothEffect; }
    public int getNoiseIterations() { return noise_iterations; }

    private class TableListener implements TableModelListener
    {

        private JLabel imgLabel;

        public TableListener()
        {
            TableModel tableModel = (TableModel)jTable2.getModel();
            createNoiseFromData(tableModel);
        }

        @Override
        public void tableChanged(TableModelEvent e)
        {
            TableModel tableModel = (TableModel)e.getSource();
            createNoiseFromData(tableModel);
        }

        private void createNoiseFromData(TableModel tableModel)
        {
            float[] givenData = new float[tableModel.getRowCount()];


            for (int i = 0; i < tableModel.getRowCount(); i++)
            {
                givenData[i] = Float.valueOf(tableModel.getValueAt(i, 1).toString());
            }

            FractalSum base = new FractalSum();
            base.setRoughness(noise_roughness = givenData[0]);
            base.setFrequency(noise_frequency = givenData[1]);
            base.setAmplitude(noise_amplitude = givenData[2]);
            base.setLacunarity(noise_lacunarity = givenData[3]);
            base.setOctaves(noise_octaves = givenData[4]);
            base.setScale(noise_scale = givenData[5]);
            base.addModulator(new NoiseModulator()
                {
                    @Override public float value(float... in)
                    {
                        return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
                    }
                });

            noiseGen = new FilteredBasis(base);
            PerturbFilter perturb = new PerturbFilter();
            perturb.setMagnitude(noise_perturbMagnitude = givenData[6]);

            OptimizedErode therm = new OptimizedErode();
            therm.setRadius(noise_erosionRadius = (int)givenData[7]);
            therm.setTalus(noise_erosionTalus = givenData[8]);

            SmoothFilter smooth = new SmoothFilter();
            smooth.setRadius(noise_smoothRadius = (int)givenData[9]);
            smooth.setEffect(noise_smoothEffect = givenData[10]);

            IterativeFilter iterate = new IterativeFilter();
            iterate.addPreFilter(perturb);
            iterate.addPostFilter(smooth);
            iterate.setFilter(therm);
            iterate.setIterations(noise_iterations = (int)givenData[11]);

            noiseGen.addPreFilter(iterate);

            buildImage();
        }

        private void buildImage()
        {
            float[] data = getHeightmap(0, 0);
            byte[] buffer = new byte[257 * 257];

            for (int i = 0; i < buffer.length; i++)
            {
                buffer[i] = (byte)(data[i] * 255);
            }

            try
            {
                File file = new File("testimage.png");

                if (file.exists())
                    file.delete();

                ImageIO.write(getGrayscale(257, buffer), "PNG", file);

                BufferedImage img = ImageIO.read(file);

                if (imgLabel != null)
                    jPanel2.remove(imgLabel);

                imgLabel = new JLabel(new ImageIcon(img));
                imgLabel.setLocation(235, 10);
                imgLabel.setSize(257, 257);

                jPanel2.add(imgLabel);
                jPanel2.repaint();

            }
            catch (IOException ex) { }
        }

        private BufferedImage getGrayscale(int width, byte[] buffer)
        {
            int height = buffer.length / width;
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = { 8 };
            ColorModel cm = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            SampleModel sm = cm.createCompatibleSampleModel(width, height);
            DataBufferByte db = new DataBufferByte(buffer, width * height);
            WritableRaster raster = Raster.createWritableRaster(sm, db, null);
            BufferedImage result = new BufferedImage(cm, raster, false, null);

            return result;
    }

        private float[] getHeightmap(int x, int z)
        {
            FloatBuffer buffer = noiseGen.getBuffer(x * (257 - 1), z * (257 - 1), 0, 257);
            return buffer.array();
        }

    }

    /**
     * This
     * method
     * is
     * called
     * from
     * within
     * the
     * constructor
     * to
     * initialize
     * the
     * form.
     * WARNING:
     * Do
     * NOT
     * modify
     * this
     * code.
     * The
     * content
     * of
     * this
     * method
     * is
     * always
     * regenerated
     * by
     * the
     * Form
     * Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jTextField1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, "Grid Size:");
        jLabel1.setToolTipText("");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, "<html>The block size determines the width and length of each TerrainQuad in world units. The patch size determines the size of each leaf in the TerrainQuad quadtree.  Both units must be a  power of 2 (e.g. 16, 32, 64, 128, 256, 512, 1024).</html>");

        jTextField2.setText("64");
        jTextField2.setName("patchSize"); // NOI18N

        jTextField1.setText("128");
        jTextField1.setName("blockSize"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, "Tile Size:");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, "Block Size:");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, "Grid Scale:");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, "<html>The grid scale acts as a multiplier for the block size set previously on the X and Z planes. The maximum height is determined by the Y plane scale.</html>");
        jLabel7.setToolTipText("");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, "Y:");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, "X:");

        jTextField3.setText("1");
        jTextField3.setName("scaleX"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, "Z:");

        jTextField4.setText("255");
        jTextField4.setToolTipText("");
        jTextField4.setName("scaleY"); // NOI18N

        jTextField5.setText("1");
        jTextField5.setToolTipText("");
        jTextField5.setName("scaleZ"); // NOI18N

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, "View Distance:");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, "<html>Set the view distance of each cardinal direction. Each value represents the amount of TerrainQuads that will be visible in that direction.</html>");

        jTextField9.setText("2");
        jTextField9.setToolTipText("");
        jTextField9.setName("vd_west"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel12, "North (-Z):");

        jTextField6.setText("2");
        jTextField6.setToolTipText("");
        jTextField6.setName("vd_east"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel13, "East (+X):");

        jTextField7.setText("2");
        jTextField7.setToolTipText("");
        jTextField7.setName("vd_north"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel14, "South (+Z):");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel15, "West (-X):");

        jTextField8.setText("2");
        jTextField8.setToolTipText("");
        jTextField8.setName("vd_south"); // NOI18N

        jLabel16.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel16, "Default Material:");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel17, "<html>Select the material that will be used for all TerrainQuads by default. Note that you can set alternative materials and other settings by subscribing to the TerrainRequested event.</html>");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSeparator2)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .addComponent(jSeparator3)
                    .addComponent(jLabel1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel5)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel10)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel16)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Configuration", jPanel1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {"Roughness",  new Float(0.95)},
                {"Frequency",  new Float(1.0)},
                {"Amplitude",  new Float(1.0)},
                {"Lacunarity",  new Float(3.12)},
                {"Octaves",  new Float(8.0)},
                {"Scale",  new Float(0.02125)},
                {"Perturb Magnitude",  new Float(0.119)},
                {"Erosion Radius*",  new Float(5.0)},
                {"Erosion Talus",  new Float(0.011)},
                {"Smooth Radius*",  new Float(1.0)},
                {"Smooth Effect",  new Float(0.7)},
                {"Overall Iterations*",  new Float(1.0)}
            },
            new String []
            {
                "Property", "Value"
            }
        )
        {
            Class[] types = new Class []
            {
                java.lang.String.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean []
            {
                false, true
            };

            public Class getColumnClass(int columnIndex)
            {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jTable2.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(jTable2);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel18, "<html>*denotes integer value only. Decimal places are truncated.</html>");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel19, "<html>Alter the values in the table above to meet your desired terrain requirements. The image represents the heightmap that is typically generated from the given data. Darker areas represent low altitude, and whiter areas represent higher altitude.</html>");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE))
                        .addGap(0, 270, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(106, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Noise Generation", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration//GEN-END:variables
}
