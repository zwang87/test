//<pre>
//Copyright 2001 Ken Perlin

package render;

//A VERY SIMPLE 3D RENDERER BUILT IN JAVA 1.0 - KEN PERLIN

/**
 Handles the manipulation, creation, and storage of geometric objects 
 for use in the renderer. <p>
 Each Geometry can have children Geometries extending in a tree 
 structure. Each Geometry has its own transform matrix, Material, 
 and a set of vertices and faces that define the object geometry. 
 Note: to add texture to a geometry other than the mesh, 
 add the u,v components to the vertices. 
 @author Ken Perlin 2001
 */

public class Geometry {

   Renderer renderer;

   private String notice = "Copyright 2001 Ken Perlin. All rights reserved.";

   /**
    Stores the geometry's material properties.
    */
   public Material material;

   /**
    Stores the bit-mask of which cameras this geometry is visible to.
    */
   public int cameraMask;

   Matrix textureMatrix;

   /**
    * Stores the name of the geometry
    */
   public String name = "";

   public int calllistnum = 0;
   public boolean animated = false;
   public boolean isVisible = true;
   public boolean isRGB = false;
   public boolean isStreak = false;
   public boolean isDashboard = false;
   public String type = "";
   public Object clientObject;

   private int glname = -1;
   private static int glnextname = 1;

   public UserListener userListener;
   public Object verticeBuffer, normalBuffer, textureBuffer;
   public int[] facesBuffer;

   public void setglname() {
      if (glname == -1) {
         glname = glnextname;
         glnextname++;
      }
   }

   public int getglname() {
      setglname();
      return glname;
   }

   public void setDashboard(boolean tf) {
      isDashboard = tf;
   }

   public Geometry setAnimated(boolean an) {
      animated = an;
      if (child != null) {
         for (int i = 0; i < child.length; i++)
            if (child[i] != null)
               child[i].setAnimated(an);
      }
      return this;
   }

   protected int nFaces = -1;
   protected int nVertices = -1;

   protected double vertexColors[][];

   public void setVertexColors(double[][] vc) { vertexColors = vc; }

   public void setNFaces(int n) { nFaces = n; }
   public void setNVertices(int n) { nVertices = n; }

   public int nVertices() { return vertices == null ? 0 : nVertices >= 0 ? nVertices : vertices.length; }
   public int nFaces() { return faces == null ? 0 : nFaces >= 0 ? nFaces : faces.length; }

   /**
    Stores the children geometries of the current geometry.
    */
   //deprecated Use access function {@link #child(int n)}
   public Geometry child[];

   private Geometry parent = null;

   /**
    Returns the parent geometry object.
    @return the parent geometry object 
    */
   public Geometry getParent() {
      return parent;
   }

   /**                                                                                                                 
    Find out whether the argument is a sub-geometry of this geometry object.                                         
    @param s Geometry to be tested                                                                                   
    @return true or false                                                                                            
    */
   public boolean contains(Geometry s) {
      for (; s != null; s = s.getParent())
         if (s == this)
            return true;
      return false;
   }

   /**
    The transform matrix of the Geometry (used to keep track of 
    translations, rotations, and scaling transformations).
    */
   public Matrix matrix = new Matrix();

   /**
    The global transform matrix used to record modifications to
    the geometry (like superquadric, or  adding slight noise 
    imperfections to the surface).
    @see #superquadric
    @see #addNoise
    */
   public Matrix globalMatrix = new Matrix();

   /**
    Index of the faces that specifies the order and vertices that 
    define individual faces. A face normal is orthogonal to the 
    surface created by counterclockwise traversal of the points 
    (right hand curl rule). First index indicates face number and 
    the second indicates vertex indeces. 
    */
   public int[][] faces;

   /**
    Array of vertices defining the geometric shape. First index 
    indicates vertex number, while the second indicates actual 
    coordinate values.
    */
   public double[][] vertices;

   /**
    A set of vertices used as an original reference when changes are 
    made to the current set of vertices.
    */
   public double[][] refVertices;
   
   /**
    Optional array of Geometry defining the object to use when transforming each vertex.
   */
   public Geometry[] bones;

   private boolean isBones;
   public Geometry[][] boneG;
   public double  [][] boneW;


   /**
    Indicates the number of rows in the mesh.
    */

   private int meshRowSize = -1;

   /**
    * if we don't have a texture then we set it as 6, if we have a texture to apply, 
    * the depth is 8
    */
   public int vertexDepth = 6;

   /**
      Sets the bone for a vertex
   */

   public void setBones(int v, int n, Geometry g[], double w[]) {
      if (! isBones || boneG == null || boneG.length < nVertices()) {
         isBones = true;
         boneG = new Geometry[nVertices()][];
         boneW = new double[nVertices()][];
      }
      boneG[v] = new Geometry[n];
      boneW[v] = new double[n];
      for (int i = 0 ; i < n ; i++) {
         boneG[v][i] = g[i];
         boneW[v][i] = w[i];
      }
   }

   public void setBone(int v, Geometry g) {
      if (bones == null)
         bones = new Geometry[nVertices()];
      bones[v] = g;
   }

   /**
      Gets the bone for a vertex
   */
   public Geometry getBone(int v) {
      if (bones == null)
         return null;
      return bones[v];
   }

   public Geometry getBoneG(int v, int i) {
      return ! isBones || boneG == null || boneG[v] == null || boneG[v].length <= i ? null : boneG[v][i];
   }

   public double getBoneW(int v, int i) {
      return ! isBones || boneW == null || boneW[v] == null || boneW[v].length <= i ? 0 : boneW[v][i];
   }

   public void unsetBones() {
      isBones = false;
      boneG = null;
      boneW = null;
   }

   /**
    Forces a recomputation of the mesh normals. 
    */
   public void recomputeMeshNormals() {
      computedMeshNormals = false;
   }

   /**
    Returns whether or nor the mesh normals have been computed yet.
    */
   public boolean computedMeshNormals() {
      return computedMeshNormals;
   }

   private boolean computedMeshNormals = false;

   /**
    Flag set when the geometry has been modified. 
    */
   protected boolean modified = false;

   /**
    Flag that determines whether both sides of the shape object are to 
    be rendered (ie. front and back). True means render both sides, false
    means just render the front. Note: even if this is false the shape
    may be rendered on both sides if its material is set to be doublesided.
    @see Material#setDoubleSided
    */
   private boolean isDoubleSided = false;

   /** 
    Adjusts the influence-weight of the pull function.
    @see #pull
    */
   public static double pullWeight = 1;
   /**
    Used to describe which children are to be transformed using 
    the pull function. 0 bits indicate no pull, 1 bits indicate 
    perform pull. 
    @see #pull
    */
   public static int pullMask = 0xffffffff;

   /** 
    Constructor sets up the local and global transformation matrices.
    */
   public Geometry() {
      Matrix.identity(matrix);
      Matrix.identity(globalMatrix);
   }

   /**
    Returns the nth child of the geometry .
    @param n the index of the child requested
    @return the shape geometry of the child. 
    */
   public Geometry child(int n) {
      if (child != null && n < child.length)
         return child[n];
      return null;
   }

   /**
    Returns the number of children of the geometry.
    */
   public int nChildren() {
      if (child == null)
         return 0;

      for (int i = 0; i < child.length; i++)
         if (child[i] == null)
            return i;

      return child.length;
   }

   public java.util.List getChildren() {

      java.util.Vector children = new java.util.Vector();

      if (child == null)
         return children; // no children, return empty List

      for (int i = 0; i < child.length; i++)
         if (child[i] != null)
            children.add(child[i]);

      return children;

   }

   /**
    * 
    * @return true if this is a descendant of geometry
    */

   public boolean isDescendant(Geometry geometry) {

      if (geometry == null)
         return false;

      // check to see if geometry is an ancestor

      Geometry ancestor = this;

      while (geometry != ancestor) {
         ancestor = ancestor.parent;
         if (ancestor == null)
            return false;
      }

      return true;

   }

   /** 
    Sets the flag indicating whether the object is double sided.
    The geometry may still be rendered on both sides if the material
    is set to be doublesided.
    @param tf value of the flag
    @return the current shape Geometry
    @see Material#setDoubleSided
    */
   public Geometry setDoubleSided(boolean tf) {
      isDoubleSided = tf;
      if (child != null) {
         for (int i = 0; i < child.length; i++)
            if (child[i] != null)
               child[i].setDoubleSided(tf);
      }

      return this;
   }

   /**
    Returns the double sided status of the shape, true if the object 
    or its material is double-sided, false otherwise.
    @return True if the object or its material is double-sided, 
    false otherwise.
    */
   public boolean isDoubleSided() {
      return isDoubleSided || material != null && material.isDoubleSided;
   }

   /**
    Adds a new child shape to this shape.
    */
   public Geometry add() {
      Geometry s = add(new Geometry());
      s.parent = this;
      s.renderer = this.renderer;
      s.material = material;
      s.isDashboard = isDashboard;
      s.isDoubleSided = isDoubleSided;
      return s;
   }

   /**
    Add an existing shape, as a child to this shape.
    @param s shape Geometry to be added
    @return s the added shape
    */
   public Geometry add(Geometry s) {
      s.parent = this;
      s.renderer = this.renderer;
      if (child == null)
         child = new Geometry[16];
      else if (child[child.length - 1] != null) {
         Geometry c[] = child;
         child = new Geometry[2 * c.length];
         for (int i = 0; i < c.length; i++)
            child[i] = c[i];
      }

      for (int i = 0; i < child.length; i++)
         if (child[i] == null) {
            child[i] = s;
            if (s.material == null)
               s.material = material;
            s.isDoubleSided = isDoubleSided;
            break;
         }
      return s;
   }

   /**
    Delete a child of a shape.
    @param s shape to be deleted from list of children
    @return the current shape geometry
    */
   public Geometry delete(Geometry s) {
      if (child != null)
         for (int i = 0; i < child.length; i++)
            if (child[i] != null && child[i] == s) {
               delete(i);
               break;
            }
      return this;
   }

   /**
    Delete the nth child of a shape.
    @param n the index of the child to be deleted
    @return the current shape geometry
    */
   public Geometry delete(int n) {
      if (child != null && n >= 0 && n < child.length) {
         if (child[n] != null)
            child[n].parent = null;
         for (; n < child.length - 1 && child[n + 1] != null; n++)
            child[n] = child[n + 1];
         child[n] = null;
      }
      return this;
   }

   /**
    Set the Material for this shape to be Material m. If the material is textured
    the geometry needs to be created with UV coordinates.
    @param m the desired Material 
    @return the current shape geometry
    */
   public Geometry setMaterial(Material m) {
      material = m;
      material.renderer = renderer;
      if (m.hasTexture() && vertices != null && vertexDepth < 8)
         if (meshRowSize > 0) {
            int M = meshRowSize;
            int N = nVertices() / (M + 1) - 1;

            double oldVertices[][] = vertices;
            vertices = new double[(M + 1) * (N + 1)][vertexDepth = 8];

            for (int k = 0; k <= N; k++)
            for (int j = 0; j <= M; j++) {
               int v = k * (M + 1) + j;
               for (int i = 0 ; i < 6 ; i++)
                  vertices[v][i] = oldVertices[v][i];
               vertices[v][6] = (double)j / M;
               vertices[v][7] = (double)k / N;
            }
         }
      if (m.hasTexture())
         this.vertexDepth = 8;
      if (child != null)
         for (int i = 0; i < child.length; i++)
            if (child[i] != null)
               child[i].setMaterial(m);
      return this;
   }

   /**
    Set the transformation matrix for this shape.
    @param m the new matrix for the shape
    */
   public void setMatrix(Matrix m) {
      matrix.copy(m);
   }

   /**
    Gets the transformation matrix for this shape.
    @return the transformation matrix for the shape
    */
   public Matrix getMatrix() {
      return matrix;
   }

   /**
    Set the x,y,z offset from the parent object.
    @param x x offset
    @param y y offset
    @param z z offset 
    */
   public void setOffset(double x, double y, double z) {
      offset[0] = x;
      offset[1] = y;
      offset[2] = z;
   }

   /**
    Get the x,y,z offset from the parent object.
    @return array of offsets [x, y, z]
    */
   public double[] getOffset() {
      return offset;
   }

   /**
    Returns a new 2-d array with a copy of vertices passed as the 
    parameter. Each vertex must consist of 6 values (x,y,z coordinates 
    + x,y,z normal). An arbitrary number of vertices is allowed.
    @param src array of vertices to be duplicated. 
    @return a new array with a copy of vertices in src
    */
   public double[][] copyVertices(double src[][]) {
      return copyVertices(src, new double[src.length][src[0].length]);
   }

   /**
    Copies and returns the contents of vertex array src to dst. 
    Each vertex must consist of 6 values (x,y,z coordinates + 
    x,y,z normal). An arbitrary number of vertices is allowed, 
    as long as src and dst both have the same dimenstions.
    @param src the original vertex array
    @param dst the destination to hold a copy of the original vertices
    @return dst, the copy of the original vertices 
    */
   public double[][] copyVertices(double src[][], double dst[][]) {
      for (int i = 0; i < src.length; i++)
         for (int j = 0; j < src[i].length; j++)
            dst[i][j] = src[i][j];
      return dst;
   }

   /**
    Sets the coordinates and normal values of vertex i.
    @param i index of the vertex to be set
    @param x x coordinate
    @param y y coordinate
    @param z z coordinate
    @param nx x normal
    @param ny y normal
    @param nz z normal
    */
   public void setVertex(int i, double x, double y, double z, double nx, double ny, double nz) {
      vertices[i][0] = x;
      vertices[i][1] = y;
      vertices[i][2] = z;
      vertices[i][3] = nx;
      vertices[i][4] = ny;
      vertices[i][5] = nz;
   }

   private void setVertex(int i, double x, double y, double z) {
      setVertex(i, x, y, z, x, y, z);
   }

   private void setVertex(int i, double entries[]) {
      for (int k = 0; k < entries.length; k++)
         vertices[i][k] = entries[k];
   }

   private void setFace(int i, int a, int b, int c) {
      faces[i][0] = a;
      faces[i][1] = b;
      faces[i][2] = c;
   }

   private void setFace(int i, int a, int b, int c, int d) {
      faces[i][0] = a;
      faces[i][1] = b;
      faces[i][2] = c;
      faces[i][3] = d;
   }

   /**
    Creates a new m by n rectangular mesh with a texture from an image file.
    @param m number of rows
    @param n number of columns
    @param fileName the file containing the source texture image.
    */
   public Geometry mesh(int m, int n, String fileName) {
      if (fileName == null)
         return mesh(m, n);

      if (m == _m && n == _n && fileName.equals(textureFileName))
         return this;

      textureFileName = fileName;
      Texture texture = null;
      try { 
         texture = new Texture(getClass().getResource("../" + fileName), "TexturedMesh");
      } catch (Exception e) { System.err.println(e); }
      setMaterial(new Material().setTexture(texture));
      return mesh(m, n);
   }

   /**
    Creates a new m by n rectangular mesh ( rows by columns ).
    If the mesh is textured, the material has to be bound to the geometry
    before calling this method.  
    @param m number of rows
    @param n number of columns
    @return the current shape geometry
    */
   public Geometry mesh(int m, int n) {
      if (m == _m && n == _n)
         return this;

      if (this.material != null && this.material.hasTexture() && this.vertexDepth >= 8)
         return mesh(m, n, true, false);
      return mesh(m, n, false, false);
   }

   /**
    * creates an m x n mesh 
    * @param m number of rows
    * @param n number of columns
    * @param createUV if true, creates the UV coordinates 
    * @param repeating if true, repeats the texture in each rectangle of the mesh
    * @return 
    */
   public Geometry mesh(int m, int n, boolean createUV, boolean repeating) {
      if (createUV)
         vertexDepth = 8;
      newRectangularMesh(m, n);

      double vertex[] = new double[this.vertexDepth];
      vertex[2] = 0;
      vertex[3] = 0;
      vertex[4] = 0;
      vertex[5] = 1;
      for (int k = 0 ; k <= n ; k++)
         for (int j = 0 ; j <= m ; j++) {
            double u = (double) j / m;
            double v = (double) k / n;
            vertex[0] = 2 * u - 1;
            vertex[1] = 2 * v - 1;
            if (createUV) {
               if (repeating) {
                  vertex[6] = j % 2;
                  vertex[7] = k % 2;
               } else {
                  vertex[6] = u;
                  vertex[7] = 1 - v;
               }
            }
            setVertex(k * (m + 1) + j, vertex);
         }
      return this;
   }

   // CUBE
   /**
    Sets the current shape geometry to a unit cube.
    @return the current shape geometry
    */
   public Geometry cube() {
      type = "cube";
      faces = cubeFaces;
      vertices = cubeVertices;
      return this;
   }

   private static int[][] cubeFaces = { { 0, 1, 2, 3 }, {
         4, 5, 6, 7 }, {
         8, 9, 10, 11 }, {
         12, 13, 14, 15 }, {
         16, 17, 18, 19 }, {
         20, 21, 22, 23 }, };

   private static double N = -1, P = 1;
   private static double[][] cubeVertices = { { N, N, N, N, 0, 0 }, {
         N, N, P, N, 0, 0 }, {
         N, P, P, N, 0, 0 }, {
         N, P, N, N, 0, 0 }, {
         P, N, N, P, 0, 0 }, {
         P, P, N, P, 0, 0 }, {
         P, P, P, P, 0, 0 }, {
         P, N, P, P, 0, 0 }, {

         N, N, N, 0, N, 0 }, {
         P, N, N, 0, N, 0 }, {
         P, N, P, 0, N, 0 }, {
         N, N, P, 0, N, 0 }, {
         N, P, N, 0, P, 0 }, {
         N, P, P, 0, P, 0 }, {
         P, P, P, 0, P, 0 }, {
         P, P, N, 0, P, 0 }, {

         N, N, N, 0, 0, N }, {
         N, P, N, 0, 0, N }, {
         P, P, N, 0, 0, N }, {
         P, N, N, 0, 0, N }, {
         N, N, P, 0, 0, P }, {
         P, N, P, 0, 0, P }, {
         P, P, P, 0, 0, P }, {
         N, P, P, 0, 0, P }, };

   // BEZELED CUBE
   /**
    Creates a bezeled cube with variable bezel of radius r.
    @param r radius of bezel
    @return the current shape geometry
    */
   public Geometry bezeledCube(double r) {
      faces = bezeledCubeFaces;
      vertices = copyVertices(bezeledCubeVertices);
      for (int i = 0; i < nVertices(); i++)
         for (int j = 0; j < 3; j++) {
            if (vertices[i][j] == n)
               vertices[i][j] = r - 1;
            if (vertices[i][j] == p)
               vertices[i][j] = 1 - r;
         }
      return this;
   }

   private static int[][] bezeledCubeFaces = { { 0, 1, 2, 3 }, {
         4, 5, 6, 7 }, {
         8, 9, 10, 11 }, {
         12, 13, 14, 15 }, {
         16, 17, 18, 19 }, {
         20, 21, 22, 23 }, {
         8, 11, 1, 0 }, {
         20, 23, 2, 1 }, {
         13, 12, 3, 2 }, {
         17, 16, 0, 3 }, {

         16, 19, 9, 8 }, {
         11, 10, 21, 20 }, {
         23, 22, 14, 13 }, {
         12, 15, 18, 17 }, {
         4, 7, 10, 9 }, {
         7, 6, 22, 21 }, {
         6, 5, 15, 14 }, {
         5, 4, 19, 18 }, {

         16, 8, 0 }, {
         11, 20, 1 }, {
         23, 13, 2 }, {
         12, 17, 3 }, {
         9, 19, 4 }, {
         21, 10, 7 }, {
         14, 22, 6 }, {
         18, 15, 5 }, };

   private static double n = -.9, p = .9;
   private static double[][] bezeledCubeVertices = { { N, n, n, N, 0, 0 }, {
         N, n, p, N, 0, 0 }, {
         N, p, p, N, 0, 0 }, {
         N, p, n, N, 0, 0 }, {
         P, n, n, P, 0, 0 }, {
         P, p, n, P, 0, 0 }, {
         P, p, p, P, 0, 0 }, {
         P, n, p, P, 0, 0 }, {

         n, N, n, 0, N, 0 }, {
         p, N, n, 0, N, 0 }, {
         p, N, p, 0, N, 0 }, {
         n, N, p, 0, N, 0 }, {
         n, P, n, 0, P, 0 }, {
         n, P, p, 0, P, 0 }, {
         p, P, p, 0, P, 0 }, {
         p, P, n, 0, P, 0 }, {

         n, n, N, 0, 0, N }, {
         n, p, N, 0, 0, N }, {
         p, p, N, 0, 0, N }, {
         p, n, N, 0, 0, N }, {
         n, n, P, 0, 0, P }, {
         p, n, P, 0, 0, P }, {
         p, p, P, 0, 0, P }, {
         n, p, P, 0, 0, P }, };

   /**
    Creates a transparent shape of unit height with a texture of a text message mapped onto it.
    @param String the text message.
    */

   Material textMaterial;

   public Geometry textString(String text, double anchor) {
      isDoubleSided = true;
      boolean isNewMaterial = false;

      double saveTransparency = material.transparency;
      if (textMaterial != null)
         textMaterial.transparency = material.transparency;
      if (textMaterial == null || ! material.equals(textMaterial)) {
         textMaterial = Material.texturedMaterial(fileName);
         textMaterial.copy(material);
         isNewMaterial = true;
         if (! isKerning) {
            computeKerning(textMaterial);
            isKerning = true;
         }
      }
      textMaterial.transparency = Math.max(material.transparency, 0.01);

      if (! this.text.equals(text) || isNewMaterial) {
         this.anchor = -10000;
         this.text = text;

         if (child != null)
            for (int i = child.length - 1 ; i >= 0 ; i--)
               delete(i);

         for (int i = 0 ; i < text.length() ; i++)
            add().textCharacter(text.charAt(i), textMaterial);
      }

      if (anchor != this.anchor) {
         this.anchor = anchor;
/*
         double xShift = -text.length() * (anchor + 1.0) * 0.5;
         for (int i = 0 ; i < text.length() ; i++)
            child(i).getMatrix().identity().translate(i + xShift, 0, 0);
*/
         double x = 0;
         for (int i = 0 ; i < text.length() ; i++) {
            int n = text.charAt(i) - ' ';
            charX[i] = x;
            double xl = cxl[n];
            double xr = cxr[n];
            x += xr - xl;
         }
         charX[text.length()] = x;

         double xShift = -charX[text.length()] * (anchor + 1.0) * 0.5;
         for (int i = 0 ; i < text.length() ; i++)
            child(i).getMatrix().identity()
                    .translate(xShift + charX[i], 0, 0)
                    .scale(charX[i + 1] - charX[i], 1, 1);
      }

      return this;
   }

   double charX[] = new double[100];

   // IMAGE OF ONE TEXT CHARACTER, AS A TEXTURE ON A SMALL MESH

   /**
    Creates a transparent square with a texture of a text character mapped onto it.
    @param char the asci character.
    */

   public Geometry textCharacter(int ch) {
      if (fontMaterial == null) {
         fontMaterial = Material.texturedMaterial(fileName);
         if (fontMaterial != null)
            fontMaterial.setAmbient(1,1,1).setTransparency(0.01);
      }
      return textCharacter(ch, fontMaterial);
   }

   static int charWidth  = 51;
   static int charHeight = 93;

   static boolean isKerning;
   static double cxl[] = new double[160];
   static double cxr[] = new double[160];

   public static void computeKerning(Material material) {

      // Compute left and right pixel of each character in font.

      for (int ch = ' ' ; ch < ' ' + 160 ; ch++) {
         int n = ch - ' ';
         int I = n % 16, J = n / 16;
         int x0 = charWidth * I;
         int x1 = charWidth * (I + 1);
         int y0 = charHeight * J;
         int y1 = charHeight * (J + 1);

         int xl = x1, xr = x0;

         // NEED TO LOOK AT EACH PIXEL IN IMAGE TO SEE IF IT IS CLEAR.

         int[] pixels = material.getTexture().packedPixels;
         for (int x = x0 ; x < x1 ; x++)
         for (int y = y0 ; y < y1 ; y++) {
            int p = pixels[x | y << 10];
            int a = p & 0xff;
            if (a > 0) {
               xl = Math.min(xl, x);
               xr = Math.max(xr, x);
            }
         }
         cxl[n] = (double)(xl - x0) / charWidth - 0.05;
         cxr[n] = (double)(xr - x0) / charWidth + 0.05;
      }
      cxl[0] = 0.2;
      cxr[0] = 0.8;
   }

   public Geometry textCharacter(int ch, Material fontMaterial) {
      setMaterial(fontMaterial);

      int _m = 2, _n = 4;
      mesh(_m, _n);

      int n = ch - ' ';
      int I = n % 16, J = n / 16; // WHERE THE CHARACTER IS WITHIN THE FONT IMAGE.

      double xl = cxl[n];
      double xr = cxr[n];

      for (int i = 0 ; i <= _m ; i++)
      for (int j = 0 ; j <= _n ; j++) {
         double v[] = vertices[i + (_m + 1) * j];
         v[0] = 1.1 * (i - 1) / _m;
         double fi = (double)i / _m;
         double fj = 1. - (double)j / _n;

         fi = xl + fi * (xr - xl);

         v[6] = charWidth  * (I + fi) / 1024.0;
         v[7] = charHeight * (J + fj) / 1024.0;
      }

      return this;
   }

   private static String fileName = "fontImage.gif";
   private static Material fontMaterial = null;

   public int charWidth () { return charWidth; }
   public int charHeight() { return charHeight; }


   // TEXTURED MESH

   /**
    Creates a square with a textured image mapped onto it.
    @param fileName the file, which must be in the class directory..
    */
   public Geometry texturedMesh(String fileName) {
      return texturedMesh(fileName, 2, 2);
   }

   public Geometry setUVRange(double u0, double v0, double du, double dv) {
      if (vertices != null && vertices[0].length >= 8)
         for (int i = 0 ; i < nVertices() ; i++) {
            vertices[i][6] = u0 + du * vertices[i][6];
            vertices[i][7] = v0 + dv * vertices[i][7];
         }
      return this;
   }

   public Geometry texturedMesh(String fileName, int m, int n) {
      setMaterial(Material.texturedMaterial(fileName));
      mesh(m, n);
      for (int i = 0 ; i < nVertices() ; i++)
         vertices[i][7] = 1 - vertices[i][7];
      return this;
   }

   // POLYGON

   /**
    Creates a polygon defined by the X and Y coordinates. 
    @param X array of x coordinates
    @param Y array of corresponding y coordinates
    @return current shape geometry
    */
   public Geometry polygon(double X[], double Y[]) {
      return polygon(X, Y, X.length);
   }

   public Geometry polygon(double X[], double Y[], int k) {

      faces = new int[1][k];
      vertices = new double[k][vertexDepth];

      for (int i = 0; i < k; i++) {
         faces[0][i] = i;
         setVertex(i, X[i], Y[i], 0, 0, 0, 1);
      }
      return this;
   }

   // k-SIDED REGULAR POLYGON APPROXIMATION TO A CIRCULAR DISK

   public Geometry partialDisk(int k, double arc) {
      faces = new int[k + 1][3];
      vertices = new double[k + 2][vertexDepth];

      for (int i = 0; i <= k; i++) {
         faces[i][0] = i;
         faces[i][1] = (i + 1) % (k + 1);
         faces[i][2] = k + 1;
         double theta = arc * i / k;
         setVertex(i, Math.cos(theta), Math.sin(theta), 0, 0, 0, 1);
      }
      setVertex(k + 1, 0, 0, 0, 0, 0, 1);
      return this;
   }

   /** 
    Creates a k-sided regular polygon approximation to a circular disk.
    @param k number of sides of the regular polygon
    @return current shape geometry
    */
   public Geometry disk(int k) {
      faces = new int[k][3];
      vertices = new double[k + 1][vertexDepth];

      for (int i = 0; i < k; i++) {
         faces[i][0] = i;
         faces[i][1] = (i + 1) % k;
         faces[i][2] = k;
         double theta = 2 * Math.PI * i / k;
         setVertex(i, Math.cos(theta), Math.sin(theta), 0, 0, 0, 1);
      }
      setVertex(k, 0, 0, 0, 0, 0, 1);
      return this;
   }

   /** 
    Creates a k-sided regular polygon approximation to an arc of a circular disk.
    @param k number of sides of the regular polygon
    @param angle0 the beginning angle of the circular arc, in radians
    @param angle1 the ending angle of the circular arc, in radians
    @return current shape geometry
    */
   public Geometry disk(int k, double angle0, double angle1) {
      faces = new int[k][3];
      vertices = new double[k + 1][vertexDepth];

      for (int i = 0; i < k; i++) {
         faces[i][0] = i;
         faces[i][1] = i + 1;
         faces[i][2] = k;
         double theta = angle0 + (angle1 - angle0) * i / (k - 1);
         setVertex(i, Math.cos(theta), Math.sin(theta), 0, 0, 0, 1);
      }
      setVertex(k, 0, 0, 0, 0, 0, 1);
      return this;
   }

   /**
    Creates a 2D gear disk with an nTeeth number of teeth.
    @param nTeeth number of teeth on the gear
    @return the current shape geometry
    @see #gear
    */
   public Geometry gearDisk(int nTeeth, int k, double s, double di) {
      double dr = k > 0 ? s : -s;
      int n = Math.abs(k) * nTeeth;
      faces = new int[n][3];
      vertices = new double[n + 1][vertexDepth];

      for (int i = 0 ; i < n ; i++) {
         faces[i][0] = i;
         faces[i][1] = (i + 1) % n;
         faces[i][2] = n;
         double theta = 2 * Math.PI * i / n;
         double r = 1.0 + dr * Math.cos(2 * Math.PI * (i + di*0.5) / k) / nTeeth;
         setVertex(i, r * Math.cos(theta), r * Math.sin(theta), 0, 0, 0, 1);
      }
      setVertex(n, 0, 0, 0, 0, 0, 1);
      return this;
   }

   private Matrix m = new Matrix();

   public Geometry partialCylinder(int k, double arc) {
      add().partialTube(k, arc);
      add().partialDisk(k, arc);
      add().partialDisk(k, arc);

      Matrix.identity(m);

      //m.scale(1, -1, -1);
      m.rotateX(Math.PI);
      m.rotateZ(-arc);

      m.translate(0, 0, 1);
      child[1].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, 1);
      child[2].setMatrix(m);

      return this;
   }

   // CYLINDER
   /** 
    Creates a k-sided polygon approximation to a cylinder.
    @param k number of subdivisions per component of the cyliner
    @return the shape geometry
    */
   public Geometry cylinder(int k) {
      type = "cylinder";

      child = null;

      add().tube(k);
      add().disk(k);
      add().disk(k);

      Matrix.identity(m);
      m.scale(1, -1, -1);
      m.translate(0, 0, 1);
      child[1].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, 1);
      child[2].setMatrix(m);

      return this;
   }

   /*
          0
          1... N
        N+1...2N
       2N+1...3N
       3N+1...4N
       4N+1
   */

   public Geometry cylinderMesh(int N) {
      if (type.equals("cylinderMesh") && vertices.length == 4 * N + 2)
         return this;

      type = "cylinderMesh";
      vertices = new double[4 * N + 2][6];
      setVertex(    0, 0,0,-1, 0,0,-1);
      setVertex(4*N+1, 0,0, 1, 0,0, 1);

      for (int i = 1 ; i <= N ; i++) {
         double theta = 2 * Math.PI * i / N;
         double s = Math.sin(theta);
         double c = Math.cos(theta);
         setVertex(    i, s,c,-1, 0,0,-1);
         setVertex(  N+i, s,c,-1, s,c, 0);
         setVertex(2*N+i, s,c, 1, s,c, 0);
         setVertex(3*N+i, s,c, 1, 0,0, 1);
      }

      faces = new int[3 * N][];
      for (int i = 0 ; i < N ; i++) {
         int i0 = i + 1, i1 = (i + 1) % N + 1;
         createFace(i, 0, i0, i1);
         createFace(i + N, N + i1, N + i0, 2*N + i0, 2*N + i1);
         createFace(i + 2 * N, 3 * N + i1, 3 * N + i0, 4 * N + 1);
      }

      return this;
   }

   void createFace(int i, int a, int b, int c) {
      faces[i] = new int[3];
      faces[i][0] = a;
      faces[i][1] = b;
      faces[i][2] = c;
   }

   void createFace(int i, int a, int b, int c, int d) {
      faces[i] = new int[4];
      faces[i][0] = a;
      faces[i][1] = b;
      faces[i][2] = c;
      faces[i][3] = d;
   }

   // TAPERED CYLINDER
   /** 
    Creates a k-sided polygon approximation to a cylinder.
    @param k number of subdivisions per component of the cyliner
    @param taper narrowing of end
    @return the shape geometry
    */
   public Geometry cylinder(int k, double taper) {
      type = "cylinder";

      child = null;

      add().tube(k, taper);
      add().disk(k);
      add().disk(k);

      Matrix.identity(m);
      m.scale(1, -1, -1);
      m.translate(0, 0, 1);
      child[1].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, 1);
      m.scale(taper, taper, 1);
      child[2].setMatrix(m);

      return this;
   }

   // GEAR
   /**
    Creates a 3D gear with nTeeth number of teeth. 
    @param nTeeth number of teeth subdivsions
    @return the shape geometry
    @see #gearDisk
    */
   public Geometry gear(int nTeeth) {
      return gear(nTeeth, 2);
   }

   public Geometry gear(int nTeeth, int k) {
      return gear(nTeeth, k, 1.0);
   }

   public Geometry gear(int nTeeth, int k, double s) {
      type = "gear";

      child = null;

      add().gearTube(nTeeth, k, s);
      add().gearDisk(nTeeth, k, s,-1);
      add().gearDisk(nTeeth, k, s, 1);

      Matrix.identity(m);
      m.scale(1, -1, -1);
      m.translate(0, 0, 1);
      child[1].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, 1);
      child[2].setMatrix(m);

      return this;
   }

   // PILL (TUBE WITH ROUNDED ENDS)
   /**
    Creates a pill with resolution k, length len, and degree of roundness 
    bulge. 
    @param k number of subdivisions of regular polygons used in 
    approximating the actual shapes.
    @param len the proportional length (height) of the cylinder
    @param bulge the degree of roundess of the edges. 1 = half sphere. 
    0 = flat surface
    @return the shape geometry
    @see #pill(int k, double len, double bulge, double taper)
    */
   public Geometry pill(int k, double len, double bulge) {
      return pill(k, len, bulge, 1);
   }

   // TAPERED PILL (TAPERED TUBE WITH ROUNDED ENDS)
   /**
    Creates a tapered pill with resolution k, length len, and degree of 
    roundness bulge and a taper of the southern hemisphere. Tapering 
    scales the southern hemisphere of the pill.
    @param k number of subdivisions of regular polygons used in 
    approximating the actual shapes.
    @param len the proportional length (height) of the cylinder
    @param bulge the degree of roundess of the edges. 1 = half sphere. 
    0 = flat surface
    @param taper additional scaling of the southern hemisphere 
    ( 1 = original size).
    @return the shape geometry
    */
   public Geometry pill(int k, double len, double bulge, double taper) {
      type = "pill";

      child = null;

      add().tube(k, taper); // CYLINDRICAL TUBE
      add().globe(k, k / 2, 0, 1, 0, .5); // NORTHERN HEMISPHERE OF A GLOBE
      add().globe(k, k / 2, 0, 1, .5, 1); // SOUTHERN HEMISPHERE OF A GLOBE

      Matrix.identity(m);
      m.scale(1, 1, len);
      child[0].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, -len);
      m.scale(1, 1, bulge);
      child[1].setMatrix(m);

      Matrix.identity(m);
      m.translate(0, 0, len);
      m.scale(taper, taper, bulge);
      child[2].setMatrix(m);

      return this;
   }

   // SPHERE

   /** 
    Creates a unit sphere out of a single cubemap-style mesh.
    @param n number of grid squares in each face of the cube.
    @return the shape geometry
    @see #globe #ball
    */
   public Geometry sphere(int n) {
      if (type.equals("sphere") && vertices.length == 6 * n * n + 2)
         return this;

      type = "sphere";

      vertices = new double[6 * n * n + 2][6];
      for (int f = 0 ; f < 6 ; f++)
      for (int j = 0 ; j <= n ; j++)
      for (int i = 0 ; i <= n ; i++) {
         int k = svi(f, i, j, n);

         double u = -1 + 2.0 * i / n;
         double v = -1 + 2.0 * j / n;
         switch (f) {
         case 0: setVertex(k,  -u,  v, -1); break;
         case 1: setVertex(k,   u,  v,  1); break;
         case 2: setVertex(k,  -1, -u,  v); break;
         case 3: setVertex(k,   1,  u,  v); break;
         case 4: setVertex(k,   v, -1, -u); break;
         case 5: setVertex(k,   v,  1,  u); break;
         }
      }

      faces = new int[6 * n * n][4];
      for (int f = 0 ; f < 6 ; f++)
      for (int j = 0 ; j < n ; j++)
      for (int i = 0 ; i < n ; i++) {
         int face[] = faces[i + j * n + f * n * n];
         face[0] = svi(f, i  , j  , n);
         face[1] = svi(f, i+1, j  , n);
         face[2] = svi(f, i+1, j+1, n);
         face[3] = svi(f, i  , j+1, n);
      }

      // PROJECT VERTICES ONTO UNIT SPHERE

      for (int i = 0 ; i < nVertices() ; i++) {
         double v[] = vertices[i];
         double x = v[0], y = v[1], z = v[2];
         double s = Math.sqrt(x * x + y * y + z * z);
         v[0] = v[3] = x / s;
         v[1] = v[4] = y / s;
         v[2] = v[5] = z / s;
      }

      computedMeshNormals = true;

      return this;
   }

   // ASSIGN A UNIQUE INDEX TO EACH VERTEX OF A CUBE SPHERE

   public int svi(int f, int i, int j, int n) {

      // MAP 2D INDICES TO 3D VOLUMETRIC COORDINATES AROUND THE CUBE

      int I = 0, J = 0, K = 0;
      switch (f) {
      case 0: I = n-i; J =   j; K =   0; break;
      case 1: I =   i; J =   j; K =   n; break;
      case 2: I =   0; J = n-i; K =   j; break;
      case 3: I =   n; J =   i; K =   j; break;
      case 4: I =   j; J =   0; K = n-i; break;
      case 5: I =   j; J =   n; K =   i; break;
      }

      if (I == 0) // LEFT FACE
         return J + K * (n+1);

      if (I == n) // RIGHT FACE
         return (n+1) * (n+1) + J + K * (n+1);

      if (J == 0) // BOTTOM FACE
         return 2 * (n+1) * (n+1) + (I-1) + K * (n-1);

      if (J == n) // TOP FACE
         return 2 * (n+1) * (n+1) + (n-1) * (n+1) + (I-1) + K * (n-1);

      if (K == 0) // BACK FACE
         return 2 * (n+1) * (n+1) + 2 * (n-1) * (n+1) + (I-1) + (J-1) * (n-1);

      if (K == n) // FRONT FACE
         return 2 * (n+1) * (n+1) + 2 * (n-1) * (n+1) + (n-1) * (n-1) + (I-1) + (J-1) * (n-1);

      return 0; // SHOULDN'T EVER GET HERE
   }

   // OCTAHEDRON

   /** 
    Creates a unit octahedron out of a single triangle mesh.
    @return the shape geometry
    */
   public Geometry octahedron() {
      type = "octahedron";

      faces = new int[8][3];
      setFace(0, 4,2,0);
      setFace(1, 1,2,4);
      setFace(2, 0,3,4);
      setFace(3, 4,3,1);
      setFace(4, 0,2,5);
      setFace(5, 5,2,1);
      setFace(6, 5,3,0);
      setFace(7, 1,3,5);

      vertices = new double[6][6];
      setVertex(0,-1, 0, 0, -1, 0, 0);
      setVertex(1, 1, 0, 0,  1, 0, 0);
      setVertex(2, 0,-1, 0,  0,-1, 0);
      setVertex(3, 0, 1, 0,  0, 1, 0);
      setVertex(4, 0, 0,-1,  0, 0,-1);
      setVertex(5, 0, 0, 1,  0, 0, 1);

      return this;
   }

   // BALL

   /** 
    Creates a unit sphere with resolution n composed of 6 n by n patches.
    @param n resolution of the meshes that compose the sphere
    @return the shape geometry
    @see #globe
    */
   public Geometry ball(int n) {
      type = "sphere";

      child = null;

      for (int i = 0; i < 6; i++) {
         Geometry s = add().newBallFace(n);
         switch (i) {
            case 1:
               s.matrix.rotateX(Math.PI / 2);
               break;
            case 2:
               s.matrix.rotateX(Math.PI);
               break;
            case 3:
               s.matrix.rotateX(-Math.PI / 2);
               break;
            case 4:
               s.matrix.rotateY(Math.PI / 2);
               break;
            case 5:
               s.matrix.rotateY(-Math.PI / 2);
               break;
         }
      }
      return this;
   }

   private Geometry newBallFace(int n) {
      newRectangularMesh(n, n);
      int N = 0;
      for (int j = 0; j <= n; j++)
         for (int i = 0; i <= n; i++) {
            double x = Math.tan(Math.PI / 4 * (j - n / 2) / (n / 2));
            double y = Math.tan(Math.PI / 4 * (i - n / 2) / (n / 2));
            double r = Math.sqrt(x * x + y * y + 1);
            x /= r;
            y /= r;
            double z = -1 / r;
            setVertex(N++, x, y, z, x, y, z);
         }
      computedMeshNormals = true;
      return this;
   }

   // GLOBE (LONGITUDE/LATITUDE SPHERE)
   /**
    Creates a longitude/latitude partitioned sphere where m and n
    specify longitude and latitude respectively.
    @param m number of longitude subdivisions
    @param n number of latitude subdivisions
    @return the shape geometry.
    @see #ball
    */
   public Geometry globe(int m, int n) {
      return globe(m, n, 0, 1, 0, 1);
   }

   // PARAMETRIC SUBSECTION OF A GLOBE
   /**
    Creates a longitude/latitude partitioned sphere delimited by ranges 
    in the east-west and north-south directions.
    @param m number of longitude subdivisions
    @param n number of latitude subdivisions
    @param uLo low end of the east-west range [0..1]
    @param uHi high end of the east-west range [0..1]
    @param vLo low end of the north-south range [0..1]
    @param vHi high end of the north-south range [0..1]
    @return the shape geometry.
    */
   public Geometry globe(int m, int n, double uLo, double uHi, double vLo, double vHi) {
      type = "sphere";
      newRectangularMesh(m, n);
      int N = 0;
      for (int j = 0; j <= n; j++)
         for (int i = 0; i <= m; i++) {
            double u = uLo + 1. * i * (uHi - uLo) / (m);
            double s = 1. * i / (m);
            double v = vLo + 1. * j * (vHi - vLo) / (n);
            double t = 1. * j / (n);

            double theta = 2 * u * Math.PI;
            double phi = (v - .5) * Math.PI;
            double x = Math.cos(phi) * Math.cos(theta);
            double y = Math.cos(phi) * Math.sin(theta);
            double z = Math.sin(phi);

            double vertex[] = { x, y, z, x, y, z, s, t};
            if (vertexDepth == 6)
               setVertex(N++, x, y, z, x, y, z);
            else
               setVertex(N++, vertex);
         }
      computedMeshNormals = true;
      return this;
   }

   public Geometry partialTube(int k, double arc) {
      double path[][] = {
         { 0, 0,-1, 1, 0, 0 },
         { 0, 0, 0, 1, 0, 0 },
         { 0, 0, 1, 1, 0, 0 }
      };
      return partialWire(k, path, 1, arc);
   }

   // TUBE
   /**
    Creates a hollow cylindrical tube with resolution k.
    @param k number of regular subdivisions approximating the surface
    @return the shape geometry
    @see #tube(int m, double taper) 
    */
   public Geometry tube(int k) {
      type = "tube";
      double path[][] = {
         { 0, 0,-1, 1, 0, 0 },
         { 0, 0, 1, 1, 0, 0 }
      };
      return wire(k, path, 1);
   }

   // TUBE MESH
   /**
    Creates a hollow cylindrical tube with circular resolution k, and linear resolution n.
    @param k number of regular subdivisions around the circle
    @param n number of regular subdivisions along the tube
    @return the shape geometry
    */
   public Geometry tubeMesh(int k, int n) {
      type = "tube";
      double path[][] = new double[n+1][6];
      for (int i = 0 ; i <= n ; i++) {
         path[i][0] = path[i][1] = path[i][4] = path[i][5] = 0;
         path[i][2] = 2.0 * i / n - 1.0;
         path[i][3] = 1;
      };
      return wire(k, path, 1);
   }

   public Geometry cappedTubeMesh(int k, int n) {
      double path[][] = new double[n+3][6];

      path[0][0] = 0;
      path[0][1] = 0;
      path[0][2] = -1.0;

      path[0][3] = 0.1;
      path[0][4] = 0;
      path[0][5] = 0;

      for (int i = 1 ; i <= n+1 ; i++) {
         path[i][0] = path[i][1] = path[i][4] = path[i][5] = 0;
         path[i][2] = 2.0 * (i-1) / n - 1.0;
         path[i][3] = 1;
      };

      path[n+2][0] = 0;
      path[n+2][1] = 0;
      path[n+2][2] = 1.0;

      path[n+2][3] = 0.1;
      path[n+2][4] = 0;
      path[n+2][5] = 0;

      return wire(k, path, 1);
   }

   // GEAR
   /** 
    Creates a hollow cylindrical geared tube with k teeth.
    @param k number of teeth
    @return the shape geometry
    */
   public Geometry gearTube(int n, int k, double s) {
      double path[][] = {
         { 0, 0,-1, 1, 0, 0 },
         { 0, 0, 0, 1, 0, 0 },
         { 0, 0, 1, 1, 0, 0 }
      };
      return gearTube(n, path, k, s);
   }

   public int getPathSize() {
      return _n + 1;
   }

   double[][] a, b;
   Curve ca, cb;
   Matrix ma, mb;
   int na, nb;

   public Geometry extrude(double[][] a, int na, double[][] b, int nb) {
      return extrude(a, null, na, b, null, nb);
   }

   public Geometry extrude(double[][] a, Matrix ma, int na, double[][] b, Matrix mb, int nb) {
      if ( isEqual(a, this.a) && isEqual(ma, this.ma) && na == this.na &&
           isEqual(b, this.b) && isEqual(mb, this.mb) && nb == this.nb )
         return this;
      return extrude(new Curve(a), ma, na, new Curve(b), mb, nb);
   }

   public Geometry extrude(Curve a, int na, Curve b, int nb) {
      return extrude(a, null, na, b, null, nb);
   }

   public Geometry extrude(Curve a, Matrix ma, int na, Curve b, Matrix mb, int nb) {
      if ( a.equals(ca) && isEqual(ma, this.ma) && na == this.na &&
           b.equals(cb) && isEqual(mb, this.mb) && nb == this.nb )
         return this;

      curveProduct(a, ma, na, b, mb, nb);
      ca = a.copy();
      cb = b.copy();
      this.ma = ma;
      this.mb = mb;
      this.na = na;
      this.nb = nb;
      return this;
   }

   boolean isEqual(double[][] a, double [][] b) {
      return false;
   }

   boolean isEqual(Matrix a, Matrix b) {
      return a == null && b == null || Matrix.same(a, b);
   }

   // CURVE PRODUCT

   public Geometry curveProduct(Curve a, Matrix ma, int na, Curve b, Matrix mb, int nb) {
      CurveProduct curveProduct = new CurveProduct(a, ma, na + 1, b, mb, nb + 1);
      newRectangularMesh(na, nb);
      for (int m = 0 ; m <= _m ; m++)
      for (int n = 0 ; n <= _n ; n++)
         Vec.copy(curveProduct.get(m, n), vertices[mn2i(m, n)]);
      computeSurfaceNormals();
      return this;
   }

   // PATH
   /**
     Creates a path, given path data.
   */

   double[][] Pn;
   int nP = -1;

   public Geometry makePath(double[][] Pn, int nP) {
      if (nP == this.nP && same(Pn, this.Pn))
         return this;

      if (this.Pn == null || this.Pn.length != Pn.length)
         //this.Pn = new double[Pn.length][Pn[0].length];
         this.Pn = new double[Pn.length][3];

      for (int n = 0 ; n < Pn.length ; n++)
        for (int k = 0 ; k < Pn[n].length ; k++)
           this.Pn[n][k] = Pn[n][k];

      return makePath(resample(Pn, nP));
   }

   public Geometry makePath(double[][] P) {
      int N = P.length;

      // MAKE SURE VERTICES ARRAY IS ALLOCATED.

      if (vertices == null || vertices.length != N)
         vertices = new double[N][6];

      // IF VERTICES VALUES ARE ALREADY UP TO DATE, THEN DO NOTHING.

      else if (P[0].length == 6 && same(vertices, P))
         return this;

      else if (P[0].length == 3) {
         boolean isSame = true;
         for (int n = 0 ; n < N && isSame ; n++)
            for (int k = 0 ; k < 3 ; k++)
               if (P[n][k] != vertices[n][k])
                  isSame = false;
         if (isSame)
            return this;
      }

      // COPY PATH DATA INTO THE VERTICES,
      // COMPUTING NORMAL VECTORS IF NEEDED.

      copy(P, vertices);
      computePathNormals(vertices);

      return this;
   }

   static void copy(double[][] src, double[][] dst) {
      int len = Math.min(src.length, dst.length);
      for (int n = 0 ; n < len ; n++) {
         int nk = Math.min(src[n].length, dst[n].length);
         for (int k = 0 ; k < nk ; k++)
            dst[n][k] = src[n][k];
      }
   }

   static void computePathTangents(double[][] dst) {
      int N = dst.length;
      if (N < 3)
         return;
      boolean isLoop = same(dst[0], dst[N - 1]);
      double sign = isLoop ? -1 : 1;
      for (int n = 0 ; n < N ; n++) {
         int n0 = isLoop ? n < N-1 ? n+1 : 1 : Math.max(0  , n - 1);
         int n1 = isLoop ? n > 0 ? n-1 : N-2 : Math.min(N-1, n + 1);
         for (int k = 0 ; k < 3 ; k++)
            wn[k] = dst[n1][k] - dst[n0][k];

         if (! isLoop && n == 0)
            for (int k = 0 ; k < 3 ; k++)
               wn[k] = 3 * dst[1][k] - 2 * dst[0][k] - dst[2][k];

         if (! isLoop && n == N-1)
            for (int k = 0 ; k < 3 ; k++)
               wn[k] = 2 * dst[N-1][k] - 3 * dst[N-2][k] + dst[N-3][k];

         for (int k = 0 ; k < 3 ; k++) {
            un[k] = dst[n1][k] - dst[n][k];
            vn[k] = dst[n][k] - dst[n0][k];
         }
         Vec.normalize(un);
         Vec.normalize(vn);
         double t = Vec.dot(un, vn);

         double scale = sign * (0.83 - (t <= 0 ? 0.96 * t : 0.33 * Math.pow(t, 0.65)));

         for (int k = 0 ; k < 3 ; k++)
            dst[n][k + 3] = scale * wn[k];
      }
   }

   static void computePathNormals(double[][] dst) {

      int N = dst.length;

      boolean isLoop = same(dst[0], dst[N - 1]);
/*
      Vec.set(w, 0, 0, 0);

      for (int n = 0 ; n < N ; n++) {
         int n0 = isLoop ? n < N-1 ? n+1 : 1 : Math.max(0  , n - 1);
         int n1 = isLoop ? n > 0 ? n-1 : N-2 : Math.min(N-1, n + 1);
         for (int k = 0 ; k < 3 ; k++) {
            un[k] = dst[n][k] - dst[n0][k];
            vn[k] = dst[n1][k] - dst[n][k];
         }
         Vec.cross(un, vn, wn);
         for (int k = 0 ; k < 3 ; k++)
            w[k] += wn[k];
      }

      double xx = w[0] * w[0], yy = w[1] * w[1], zz = w[2] * w[2];
      if (xx + yy + zz < 0.01) {
         int a = xx > yy && xx > zz ? 1 : yy > zz ? 2 : 0;
         for (int k = 0 ; k < 3 ; k++)
            w[k] = k == a ? 1.0 : 0.0;
      }

      Vec.normalize(w);
*/
      Vec.set(w, 0, 0, -1);

      for (int n = 0 ; n < N ; n++) {
         int n0 = isLoop ? n < N-1 ? n+1 : 1 : Math.max(0  , n - 1);
         int n1 = isLoop ? n > 0 ? n-1 : N-2 : Math.min(N-1, n + 1);
         for (int k = 0 ; k < 3 ; k++)
            un[k] = dst[n1][k] - dst[n0][k];
         Vec.cross(un, w, wn);
         Vec.normalize(wn);
         for (int k = 0 ; k < 3 ; k++)
            dst[n][k + 3] = wn[k];
      }
   }

   String toString(double[] v) {
      return ((int)(100 * v[0]) / 100.) + " " +
             ((int)(100 * v[1]) / 100.) + " " +
             ((int)(100 * v[2]) / 100.) ;
   }

   // TORUS
   /**
    Creates a torus with resolution m by n and radius r. 
    @param m number of latitude subdivisions
    @param n number of longitude subdivisions
    @param r radius of the ring
    @return the shape geometry
    */

   public Geometry torus(int m, int n, double r) {
      type = "torus";
      if (m != _m || n != _n || r != _r) {
         extrusion(makeCircle(m, r), makeCircle(n, 1.0));
         _r = r;
      }
      return this;
   }

   public Geometry partialWire(int m, int n, double key[][], double r, double arc) {
      return partialWire(m, resample(key, n), r, arc);
   }

   // EXTRUDE A WIRE ALONG A PATH

   /**
    Extrudes a wire/jointed-tube along the specified keypoints,
    with radius r, m segments approximating roundness, and n segments
    approximating the path along the key points.
    @param m number of sides of the regular polygon approximating the
    circular cross section of the wire
    @param n number of sections approximating the path along the
    key points.
    @param key the list of points defining the path
    @param r radius of the wire
    @return the shape of the geometry
    */
   public Geometry wire(int m, int n, double key[][], double r) {
      return wire(m, resample(key, n), r);
   }

   public Geometry partialWire(int m, double path[][], double r, double arc) {
      return extrusion(makeWedge(m, r, arc), path);
   }

   /**
    Extrudes a wire/jointed-tube along the specified keypoints,
    with radius r, m segments approximating roundness, along the path
    determined by the key points.
    @param m number of sides of the regular polygon approximating the
    circular cross section of the wire
    @param path the list of key points defining the path
    @param r radius of the wire
    @return the shape of the geometry
    */
   public Geometry wire(int m, double path[][], double r) {
      return extrusion(makeCircle(m, r), path);
   }

   /**
    Creates a jointed gear-tube with m number of teeth, along the specified 
    path.
    @param m number of teeth of the gear
    @param path the set of points defining the key joints of the wire tube.
    @return the shape geometry
    */
   public Geometry gearTube(int m, double path[][], int k, double s) {
      return extrusion(makeGear(m, k, s), path);
   }

   /**
    Extrudes a path along another path where O defines the path of the 
    cross-section and P defines the path of the wire.
    @param O a set of vertices and normals defining the cross section
    @param P a set of vertices and normals defining the path of the wire.
    @return the shape geometry
    */

   double U[] = new double[3];
   double V[] = new double[3];
   double W[] = new double[3];
   double[][] _GV, _V0, _V1;
   Matrix _M, normat, _M0, _M1;
   Geometry p0, p1;
   boolean didExtrusion = false;

   public void forceExtrusion() {
      p0 = null;
   }

   public boolean didExtrusion() {
      return didExtrusion;
   }

   public Geometry extrusion(Geometry p0, Geometry p1) {
      didExtrusion = false;

      if (p0 == null || p1 == null || p0.vertices == null || p1.vertices == null)
         return this;

      p0.computeGlobalMatrix();
      p1.computeGlobalMatrix();

      if ( p0 == this.p0 && same(_V0, p0.vertices) && Matrix.same(_M1, p1._M) &&
           p1 == this.p1 && same(_V1, p1.vertices) && Matrix.same(_M0, p0._M) )
         return this;

      p0.computeGlobalVertices();
      p1.computeGlobalVertices();
      extrusion(p0._GV, p1._GV);
      didExtrusion = true;

      this.p0 = p0;
      this.p1 = p1;

      _V0 = copyVertices(p0.vertices, vertices(_V0, p0.vertices.length));
      _V1 = copyVertices(p1.vertices, vertices(_V1, p1.vertices.length));

      (_M0 = (_M0 == null ? new Matrix() : _M0)).copy(p0._M);
      (_M1 = (_M1 == null ? new Matrix() : _M1)).copy(p1._M);

      return this;
   }

   void computeGlobalMatrix() {
      if (_M == null)
         _M = new Matrix();
      computeGlobalMatrix(_M);
   }

   void computeGlobalVertices() {
      _GV = vertices(_GV, vertices.length);
      transformVertices(_M, _GV);
   }

   double[][] vertices(double[][] v, int length) {
      if (v == null || v.length != length)
         v = new double[length][6];
      return v;
   }

   void transformVertices(Matrix m, double[][] dst) {
      for (int n = 0 ; n < vertices.length ; n++)
         m.transform(vertices[n][0], vertices[n][1], vertices[n][2], dst[n]);

      if (normat == null)
         normat = new Matrix();
      Renderer.createNormat(m, normat);

      for (int n = 0 ; n < vertices.length ; n++) {
         for (int k = 0 ; k < 3 ; k++)
            w[k] = vertices[n][k + 3];
         double s0 = Vec.norm(w);
         normat.transform(vertices[n][3], vertices[n][4], vertices[n][5], w);
         double s1 = Vec.norm(w);
         for (int k = 0 ; k < 3 ; k++)
            dst[n][k + 3] = w[k] * s0 / s1;
      }
   }

   public Geometry extrusion(double[][] O, double[][] P) {
      int m = O.length - 1;
      int n = P.length - 1;
      newRectangularMesh(m, n);

      boolean loop = same(P[0], P[n]);

      int N = 0;
      for (int j = 0; j <= n; j++) {
         for (int k = 0; k < 3; k++) {
            U[k] = P[j][k + 3];
            W[k] = j < n ? P[j + 1][k] - P[j][k]
                         : loop ? P[1][k] - P[0][k]
                                : P[n][k] - P[n - 1][k] ;
         }
         double radius = Vec.norm(U);
         computeCrossVectors(U, W, V);
         for (int i = 0; i <= m; i++) {
            double x = O[i][0];
            double y = O[i][1];
            double z = O[i][2];
            for (int k = 0; k < 3; k++)
               vertices[N][k] = (P[j][k] + radius * (x * U[k] - y * V[k] + z * W[k]));
            N++;
         }
      }

      if (loop)
         for (int i = 0; i <= nVertices(); i++)
            for (int k = 0; k < 3; k++)
               if (indx(m, n, i, n) < nVertices())
                  vertices[indx(m, n, i, n)][k] = vertices[indx(m, n, i, 0)][k];

      return this;
   }

   private void transformVertexCompletely(double src[], Matrix m, double dst[]) {
      for (int i = 0; i < 3; i++) {
         dst[i] = src[0] * m.get(i, 0) + src[1] * m.get(i, 1) + src[2] * m.get(i, 2) + m.get(i, 3);
         dst[i + 3] = src[3] * m.get(i, 0) + src[4] * m.get(i, 1) + src[5] * m.get(i, 2) + m.get(i, 3);
      }

      double w = src[0] * m.get(3, 0) + src[1] * m.get(3, 1) + src[2] * m.get(3, 2) + m.get(3, 3);
      for (int i = 0; i < 3; i++)
         dst[i] /= w;
   }

   /**
    Normalizes W and U, computes their cross product into V, normalizes V,
    computes the cross product of V and W into U, and normalizes U.
    @param W vector
    @param U vector
    @param V vector
    */
   private void computeCrossVectors(double W[], double U[], double V[]) {
      Vec.normalize(W);
      Vec.normalize(U);
      Vec.cross(W, U, V);
      Vec.normalize(V);
      Vec.cross(V, W, U);
      Vec.normalize(U);
   }

   int _m;     // number of columns for meshes.
   int _n;     // number of rows for meshes.
   double _r;  // radius for things that have radii.

   /**
    Returns the number of columns in the mesh.
    */
   public int getMeshCols() {
      return _m;
   }

   /**
    Returns the number of rows in the mesh.
    */
   public int getMeshRows() {
      return _n;
   }

   private double[] V(int i, int j) {
      int n = indx(_m, _n, i, j);
      n = Math.min(n, nVertices() - 1);
      return vertices[n];
   }

   /**
    Computes normals for each vertex of each face of the polyhedron shape.
    */
   public void computePolyhedronNormals() {
      for (int f = 0; f < nFaces() ; f++) {
         int face[] = faces[f];
         if (face != null) {
            for (int j = 0; j < 3; j++) {
               int a = face[0], b = face[1], c = face[2];
               A[j] = vertices[b][j] - vertices[a][j];
               B[j] = vertices[c][j] - vertices[b][j];
            }
            Vec.cross(A, B, C);
            Vec.normalize(C);
            for (int k = 0 ; k < face.length && face[k] >= 0 ; k++)
               for (int j = 0; j < 3; j++)
                  vertices[face[k]][3 + j] = C[j];
         }
      }
   }

   private int indx(int m, int n, int i, int j) {
      return ((j + n + 1) % (n + 1)) * (m + 1) + ((i + m + 1) % (m + 1));
   }

   /**
    Creates a tapered tube, where m denotes the number of steps around 
    the circle and taper indicates the additional scaling of the southern 
    end of the tube.
    @param m the number of steps around the circle 
    ( regular polygonal approximation)
    @param taper the scale factor of the southern end of the tube.
    @return the shape geometry
    @see #tube(int m)
    */
   public Geometry tube(int m, double taper) {
      double T[] = { -1, 1};
      double C[] = { 1, taper};
      return latheGen(m, T, C, false);
   }

   // LATHE (OBJECT FORMED ON A LATHE)

   /**
    Extrudes a curve of varying radius around a circle.<p>
    Algorithm:<p>
    The path is first expanded out to a smooth curve,
    so that it is sampled evenly in n steps.  The curve
    preserves local maxima and minima in radius (eg:
    if a keypoint has a greater radius than both
    its neighbors, then that point will be a local
    maximum in the resulting curve).
    <p>
    Then this expanded path is extruded around a circle.
    
    @param m the number of steps around the circle
    @param n the number of steps to sample along the path
    @param Z keypoints along the curve that contain successive z coordinates
    @param R keypoints along the curve that contain successive radii
    @return the shape geometry
    */
   public Geometry lathe(int m, int n, double Z[], double R[]) {

      double T[] = new double[n + 1];
      double C[] = new double[n + 1];
      makeCurve(Z, R, T, C);
      return latheGen(m, T, C, true);
   }

   /**
    Extrudes a curve of varying radius around a circle of resolution m
    defined by the the coordinates in T (containing the successive z 
    coordinates) and C (containing the successive radii).
    @param m number of segments approximating the roundness of the tube
    @param T keypoints along the curve that contain successive z coordinates
    @param C keypoints along the curve that contain successive radii
    @param round               // good question. ij
    @return the shape geometry
    @see #lathe(int m, int n, double[] Z, double[] R)
    */
   public Geometry latheGen(int m, double T[], double C[], boolean round) {

      int n = T.length - 1;

      newRectangularMesh(m, n);

      for (int i = 0; i <= n; i++)
         for (int j = 0; j <= m; j++) {
            double theta = 2 * Math.PI * j / m;
            double x = Math.cos(theta);
            double y = Math.sin(theta);
            double z = T[i];
            double r = C[i];
            double sign = T[0] < T[n] ? 1 : -1;
            double dr;
            if (round)
               dr = i == 0 ? -sign : i == n ? sign : (C[i + 1] - C[i - 1]) / (2 * r);
            else
               dr = (C[1] - C[0]) / r;
            double nn[] = { r * x, r * y, dr};
            Vec.normalize(nn);
            setVertex(i * (m + 1) + j, r * x, r * y, z, nn[0], nn[1], nn[2]);
         }
      computedMeshNormals = true;

      return this;
   }

   private void newRectangularMesh(int m, int n) {

      meshRowSize = m;

      _m = m;
      _n = n;

      faces = new int[m * n][4];
      vertices = new double[(m + 1) * (n + 1)][vertexDepth];

      for (int k = 0; k < n; k++)
      for (int j = 0; j < m; j++) {
         int f = k * m + j;
         int v = k * (m + 1) + j;
         faces[f][0] = v;
         faces[f][1] = v + 1;
         faces[f][2] = v + m + 1 + 1;
         faces[f][3] = v + m + 1;
      }

      if (vertexDepth >= 8)
         for (int k = 0; k <= n; k++)
         for (int j = 0; j <= m; j++) {
            int v = k * (m + 1) + j;
            vertices[v][6] = (double)j / m;
            vertices[v][7] = (double)k / n;
         }

      computedMeshNormals = false;
   }

   /**
    Creates a smooth path composed of n subsegments that passes
    through the desired key points (uses hermite spline).
    @param n number of subdivisions in path
    @param key a set of points (inluding normals) defining  key positions
    @return array of n+1 points defining the path 
    */
   public static double[][] resample(double key[][], int n) {
      double P[][] = new double[n + 1][6];
      resample(key, P, n);
      return P;
   }

   /**
    Creates a smooth path composed of n subsegments that passes
    through the desired key points (uses hermite spline).
    @param n number of subdivisions in path
    @param key a set of points (inluding normals) defining  key positions
    @param array of n+1 points defining the path 
   */
   public static void resample(double[][] src, double[][] dst, int nDst) {
      resample(src, src.length, dst, nDst);
   }

   public static void resample2(double[][] src, int srcLen, double[][] dst, int dstLen) {
      Curve curve = new Curve();
      for (int n = 0 ; n < srcLen ; n++)
         for (int j = 0 ; j < src[n].length ; j++)
            curve.setValue(n, j, src[n][j]);

      for (int n = 0 ; n < dstLen ; n++) {
         double t = (double)n * srcLen / dstLen;
         for (int j = 0 ; j < dst[n].length ; j++)
            dst[n][j] = curve.eval(t, j);
      }
   }

   public static void resample(double[][] src, int srcLen, double[][] dst, int dstLen) {
      if (srcLen == 0 || dstLen == 0)
         return;

      double[][] key = src;

      if (src[0].length < 6) {
         key = new double[srcLen][6];
         copy(src, key);
         computePathTangents(key);
      }

      boolean isLoop = same(key[0], key[srcLen - 1]);

      int kPrev = -1;
      for (int i = 0 ; i <= dstLen ; i++) {
         double f = (srcLen - 1) * i / (dstLen - 0.01);
         int k = (int) f;
         int k1 = isLoop ? Math.min(k + 1, srcLen - 1) : k < srcLen - 1 ? k + 1 : 1;
         for (int j = 0; j < 3; j++)
            dst[i][j] = hermite(0, 1, key[k][j    ], key[k1][j    ],
                                      key[k][j + 3], key[k1][j + 3], f % 1.0);
      }
   }

   /**     
    Creates a geared path (triangular extrusions from a circle) with 
    a given number of teeth. 
    @param nTeeth number of teeth subdivisions in the circle
    @return the set of vertices (including normals) that define the shape
    */
   public static double[][] makeGear(int nTeeth, int k, double s) {
      double dr = k > 0 ? s : -s;
      int n = Math.abs(k) * nTeeth;
      double P[][] = new double[n + 1][6];
      for (int i = 0; i <= n ; i++) {
         double theta = 2 * Math.PI * i / n;
         double r = 1.0 + dr * Math.cos(2 * Math.PI * (i + 0.5) / k) / nTeeth;

         P[i][0] = r * Math.cos(theta);
         P[i][1] = r * Math.sin(theta);
         P[i][2] = 0;

         P[i][3] = Math.cos(theta - Math.PI / 4);
         P[i][4] = Math.sin(theta - Math.PI / 4);
         P[i][5] = 0;
      }
      return P;
   }

   public static double[][] makeWedge(int n, double radius, double arc) {
      double P[][] = new double[n + 6][6];
      for (int i = 0; i <= n; i++) {
         double theta = arc * i / n;
         double cos = Math.cos(theta);
         double sin = Math.sin(theta);

         P[i][0] = radius * cos; // LOCATION
         P[i][1] = radius * sin;
         P[i][2] = 0;

         P[i][3] = cos; // NORMAL DIRECTION
         P[i][4] = sin;
         P[i][5] = 0;
      }

      P[n+1][0] = P[n][0];
      P[n+1][1] = P[n][1];
      P[n+1][2] = 0;

      P[n+1][3] = Math.cos(arc);
      P[n+1][4] = Math.sin(arc);
      P[n+1][5] = 0;

      P[n+2][0] = 0;
      P[n+2][1] = 0;
      P[n+2][2] = 0;

      P[n+2][3] = Math.cos(arc);
      P[n+2][4] = Math.sin(arc);
      P[n+2][5] = 0;

      P[n+3][0] = 0;
      P[n+3][1] = 0;
      P[n+3][2] = 0;

      P[n+3][3] = 1;
      P[n+3][4] = 0;
      P[n+3][5] = 0;

      for (int j = 0 ; j < 3 ; j++)
         P[n+4][j] = P[0][j];
      P[n+4][3] = -P[0][4];
      P[n+4][4] =  P[0][3];
      P[n+4][5] =  0;

      P[n+5][0] = 1;
      P[n+5][1] = 0;
      P[n+5][2] = 0;

      P[n+5][3] = 0;
      P[n+5][4] = -1;
      P[n+5][5] = 0;

      return P;
   }

   public static double[][] makeCircle(int n, double radius) {
      return makeCircle(n, radius, 0);
   }

   /**
    Creates a regular n-sided polygonal approximation to a circle 
    with given radius.
    @param n number of sides in the polygon
    @param radius the radius of the circle
    @param phaseOffset an offset to the angular phase
    @return the set of vertices defining the polygon. 
    [point number][x, y, z coordinates]
    */
   public static double[][] makeCircle(int n, double radius, double phaseOffset) {
      double P[][] = new double[n + 1][6];
      for (int i = 0; i <= n; i++) {
         double theta = 2 * Math.PI * i / n + phaseOffset;
         double cos = Math.cos(theta);
         double sin = Math.sin(theta);

         P[i][0] = radius * cos; // LOCATION
         P[i][1] = radius * sin;
         P[i][2] = 0;

         P[i][3] = cos; // NORMAL DIRECTION
         P[i][4] = sin;
         P[i][5] = 0;
      }
      return P;
   }

   /** 
    Makes a smooth curve        
    */
   public static void makeCurve(double X[], double Y[], double T[], double C[]) {
      double S[] = new double[X.length]; // SLOPE
      int n = X.length;

      for (int i = 1; i < n - 1; i++)
         S[i] = (Y[i] >= Y[i - 1]) == (Y[i] >= Y[i + 1]) ? 0 : ((Y[i + 1] - Y[i]) * (X[i] - X[i - 1]) + (Y[i] - Y[i - 1]) * (X[i + 1] - X[i])) / ((X[i + 1] - X[i - 1]) * (X[i + 1] - X[i - 1]));

      S[0] = 2 * (Y[1] - Y[0]) / (X[1] - X[0]) - S[1];
      S[n - 1] = 2 * (Y[n - 1] - Y[n - 2]) / (X[n - 1] - X[n - 2]) - S[n - 2];

      int k = C.length;
      for (int j = 0; j < k; j++) {
         double t = j / (k - .99);
         double x = X[0] + t * (X[n - 1] - X[0]);
         int i = 0;
         for (; i < n - 1; i++)
            if (x >= X[i] != x >= X[i + 1])
               break;
         T[j] = x;
         C[j] = hermite(X[i], X[i + 1], Y[i], Y[i + 1], S[i], S[i + 1], x);
      }
   }

   private static double hermite(double x0, double x1, double y0, double y1, double s0, double s1, double x) {
      double t = (x - x0) / (x1 - x0);
      double s = 1 - t;
      return y0 * s * s * (3 - 2 * s) + s0 * (1 - s) * s * s - s1 * (1 - t) * t * t + y1 * t * t * (3 - 2 * t);
   }

   static protected boolean same(double a[], double b[]) {
      if (a == null || b == null || a.length != b.length)
         return false;
      for (int n = 0 ; n < a.length ; n++)
         if (Math.abs(a[n] - b[n]) >= 0.01)
            return false;
      return true;
   }

   protected boolean same(double a[][], double b[][]) {
      if (a == null || b == null || a.length != b.length)
         return false;
      for (int n = 0 ; n < a.length ; n++)
         if (! same(a[n], b[n]))
            return false;
      return true;
   }

   /**
    Deforms the current shape geometry in a similar manner to a quadric, 
    but is just controlled by two parameters (p-center side "inflation", 
    h-corner "inflation").  <p>
    @param p controls the amount of scaling of the centers of sides of
    the quadric ( p >= 0 )
    @param h controls the amount of scaling of the corners relative to the
    rest of the shape ( h >= 0 )
    @return the shape geometry
    @see #superquadric(Matrix mat, double p, double h)
    */
   public Geometry superquadric(double p, double h) {
      superquadric(globalMatrix, p, h);
      return this;
   }

   /**
    Deforms the current shape geometry in a similar manner to a quadric, 
    but is just controlled by two parameters (p-center side "inflation", 
    h-corner "inflation").  <p>
    @param mat the transformation matrix basis for the superquadric
    @param p controls the amount of scaling of the centers of sides of
    the quadric ( p >= 0 )
    @param h controls the amount of scaling of the corners relative to the
    rest of the shape ( h >= 0 )
    @see #superquadric(double p, double h)
    */

   static double w[] = new double[3];
   static double un[] = new double[3];
   static double vn[] = new double[3];
   static double wn[] = new double[3];
   Matrix inv, matn;

   public void superquadric(Matrix mat, double p, double h) {

      if (child != null) {
         for (int i = 0; i < child.length; i++)
            if (child[i] != null) {
               Matrix tmp = new Matrix();
               tmp.copy(mat);
               tmp.preMultiply(child[i].matrix);
               child[i].superquadric(tmp, p, h);
            }
         return;
      }

      double v[][] = vertices, x, y, z;
      if (inv == null)
         inv = new Matrix();
      inv.invert(mat);
      for (int k = 0; k < v.length; k++) {
         transform(v[k], mat, w);
         x = Math.abs(w[0]);
         y = Math.abs(w[1]);
         z = Math.abs(w[2]);
         double t = Math.pow(Math.pow(x, p) + Math.pow(y, p) + Math.pow(z, p), 1 / p);
         w[0] /= t;
         w[1] /= t;
         w[2] /= t;

         if (h > 0) {
            t = Math.pow(w[0] * w[0] + w[1] * w[1] + w[2] * w[2], h);
            w[0] *= t;
            w[1] *= t;
            w[2] *= t;
         }

         transform(w, inv, v[k]);
      }
      computedMeshNormals = false;
   }

   /**
    Adds noise to the global transformation matrix.
    @param freq the frequency of noise
    @param ampl the amplitude of noise
    @see #addNoise(double freq, double ampl)
    */
   public void addNoise(double freq, double ampl) {
      addNoise(globalMatrix, freq, ampl);
   }

   /**
    Adds noise (with frequency freq and amplitude ampl) to the desired matrix mat.
    @param mat matrix to be modified
    @param freq frequency of noise
    @param ampl amplitude of noise
    */
   public void addNoise(Matrix mat, double freq, double ampl) {

      if (child != null) {
         for (int i = 0; i < child.length; i++)
            if (child[i] != null) {
               Matrix tmp = new Matrix();
               tmp.copy(mat);
               tmp.preMultiply(child[i].matrix);
               child[i].addNoise(tmp, freq, ampl);
            }
         return;
      }

      double v[][] = vertices, x, y, z;
      if (matn == null)
         matn = new Matrix();
      matn.copy(mat);
      matn.set(0, 3, 0);
      matn.set(1, 3, 0);
      matn.set(2, 3, 0);
      if (inv == null)
         inv = new Matrix();
      inv.invert(mat);
      for (int k = 0; k < v.length; k++) {
         transform(v[k], mat, w);
         for (int j = 0; j < 3; j++)
            vn[j] = v[k][3 + j];
         transform(vn, matn, wn);
         x = freq * w[0];
         y = freq * w[1];
         z = freq * w[2] + 100;
         double t = ampl * Noise.noise(x, y, z);
         for (int j = 0; j < 3; j++)
            w[j] += t * wn[j];
         transform(w, inv, v[k]);
      }
      computedMeshNormals = false;
   }

   /** 
    Origin of the noise space, default [0, 0, 0].
    */
   public double noiseOrigin[] = { 0, 0, 0};

   /**
    Displaces the shape geometry (each vertex) and its children by noise 
    determined by frequency and amplitude.
    @param freq frequency of noise
    @param ampl amplitude of noise
    @see #addImprovedNoise(double freq, double ampl)
    */
   public void displaceByImprovedNoise(double freq, double ampl) {
      if (child != null)
         for (int i = 0; i < child.length && child[i] != null; i++)
            child[i].displaceByImprovedNoise(freq, ampl);

      if (vertices != null) {
         double v[][] = vertices, x, y, z, s;
         for (int k = 0; k < v.length; k++) {
            x = freq * (v[k][0] + noiseOrigin[0]);
            y = freq * (v[k][1] + noiseOrigin[1]);
            z = freq * (v[k][2] + noiseOrigin[2]);
            s = ampl * ImprovedNoise.noise(x, y, z);
            if (v[k][3] * v[k][3] + v[k][4] * v[k][4] + v[k][5] * v[k][5] < 2) {
               v[k][0] += s * v[k][3];
               v[k][1] += s * v[k][4];
               v[k][2] += s * v[k][5];
            }
         }
      }
      computedMeshNormals = false;
   }

   /**
    Adds noise to each vertex in the geometry and recursively to its
    children using improvedNoise.
    @param freq desired frequency of noise
    @param ampl desired amplitude of noise
    @see #displaceByImprovedNoise(double freq, double ampl)
    @see ImprovedNoise
    */
   public void addImprovedNoise(double freq, double ampl) {
      if (child != null) {
         for (int i = 0; i < child.length && child[i] != null; i++)
            child[i].addImprovedNoise(freq, ampl);
         return;
      }
      double v[][] = vertices, x, y, z;
      for (int k = 0; k < v.length; k++) {
         x = freq * (v[k][0] + noiseOrigin[0]);
         y = freq * (v[k][1] + noiseOrigin[1]);
         z = freq * (v[k][2] + noiseOrigin[2]);
         v[k][0] += ampl * ImprovedNoise.noise(x, y, z);
         v[k][1] += ampl * ImprovedNoise.noise(y, z, x);
         v[k][2] += ampl * ImprovedNoise.noise(z, x, y);
      }
      computedMeshNormals = false;
   }

   /**
    Attempts to sew the s geometry to the current shape geometry if 
    the a and b parameters are both nonzero.  // ask ken for details. ij
    @param s geometry mesh to be sewed
    @param a 
    @param b
    @see #sew(Geometry s, int a, int b, Matrix m)
    */
   public void sew(Geometry s, int a, int b) {
      sew(s, a, b, null);
   }

   /**
    Attempts to sew the s geometry to the current shape geometry if 
    the a and b parameters are both nonzero according to transformation 
    matrix m.   // ask ken for details. ij
    @param s geometry mesh to be sewed
    @param a 
    @param b
    @param m matrix that modifies the sewing transformation
    @see #sew(Geometry s, int a, int b)
    */
   public void sew(Geometry s, int a, int b, Matrix m) {
      int am = meshRowSize, an = nVertices() / (am + 1) - 1;
      int bm = s.meshRowSize, bn = s.nVertices() / (bm + 1) - 1;
      int j1 = a == 0 ? 0 : an * (am + 1);
      int j2 = b == 0 ? 0 : bn * (bm + 1);
      if (am >= bm)
         for (int i = 0; i <= am; i++)
            sewVertex(vertices[j1 + i], m, s.vertices[j2 + i * bm / am]);
      else
         for (int i = 0; i <= bm; i++)
            sewVertex(vertices[j1 + i * am / bm], m, s.vertices[j2 + i]);
   }

   private void sewVertex(double v1[], Matrix m, double v2[]) {
      if (m != null)
         transformVertex(v1, m, v2);
      else
         copyVertex(v1, v2);
   }

   /**
    Copies the coordinates and normal values of the original vertex 
    to the target vertex. 
    @param src original source vertex
    @param dst target destination vertex
    */
   public void copyVertex(double src[], double dst[]) {
      for (int j = 0; j < dst.length; j++)
         dst[j] = src[j];
   }

   /**
    Applies the transformation matrix m to the source vertex src and stores 
    the result in the desination vertex dst.
    @param src vertex to be transformed
    @param m the matrix defining the transformation
    @param dst the vertex resulting from the transformation
    */
   void transformVertex(double src[], Matrix m, double dst[]) {
      for (int i = 0; i < 3; i++)
         dst[i] = src[0] * m.get(i, 0) + src[1] * m.get(i, 1) + src[2] * m.get(i, 2) + m.get(i, 3);
      dst[3] = src[3];
      dst[4] = src[4];
      dst[5] = src[5];

      double w = src[0] * m.get(3, 0) + src[1] * m.get(3, 1) + src[2] * m.get(3, 2) + m.get(3, 3);
      dst[0] /= w;
      dst[1] /= w;
      dst[2] /= w;
   }

   /**
    Deforms a geometric shape according to the beginning, middle, and 
    end parameters in each dimension. For each dimesion the three 
    parameters indicate the amount of deformation at each position. <p>
    0 - beginning, 1 - middle, 2 - end. To indicate infinity (a constant
    transformation) set two adjacent parameters to the same value.
    Setting all three parameters to the same value transforms the 
    shape geometry consistently across the entire axis of the parameters.
    @param m the object's transformation matrix
    @param x0 location of beginning of deformation along the x axis
    @param x1 location of beginning of deformation along the x axis
    @param x2 location of beginning of deformation along the x axis
    @param y0 location of beginning of deformation along the y axis
    @param y1 location of beginning of deformation along the y axis
    @param y2 location of beginning of deformation along the y axis
    @param z0 location of beginning of deformation along the z axis
    @param z1 location of beginning of deformation along the z axis
    @param z2 location of beginning of deformation along the z axis
    @return 1 if pull operation was successful, 0 otherwise 
    */
   public int pull(Matrix m, double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1, double z2) {
      Matrix tmp1 = new Matrix();
      Matrix tmp2 = new Matrix();

      tmp1.identity();
      tmp2.invert(globalMatrix);
      m.preMultiply(tmp2);

      return pull(tmp1, tmp2, x0, x1, x2, y0, y1, y2, z0, z1, z2);
   }

   final double UNDEFINED = .001234;
   double vCache[][];

   private double A[] = { 0, 0, 0}, B[] = { 0, 0, 0}, C[] = { 0, 0, 0};
   private Matrix inverseOldRelMatrix = new Matrix();

   /** 
    Deforms a geometric shape according to the beginning, middle, and 
    end parameters in each dimension. For each dimesion the three 
    parameters indicate the amount of deformation at each position. <p>
    0 - beginning, 1 - middle, 2 - end. To indicate infinity (a constant
    transformation) set two adjacent parameters to the same value.
    Setting all three parameters to the same value transforms the 
    shape geometry consistently across the entire axis of the parameters.
    
    @param oldRelMatrix the object's initial transformation 
    (usually the identity matrix) 
    @param newRelMatrix the object's final transformation matrix
    @param x0 location of beginning of deformation along the x axis
    @param x1 location of beginning of deformation along the x axis
    @param x2 location of beginning of deformation along the x axis
    @param y0 location of beginning of deformation along the y axis
    @param y1 location of beginning of deformation along the y axis
    @param y2 location of beginning of deformation along the y axis
    @param z0 location of beginning of deformation along the z axis
    @param z1 location of beginning of deformation along the z axis
    @param z2 location of beginning of deformation along the z axis
    @return 1 if pull operation was successful, 0 otherwise
    */
   public int pull(Matrix oldRelMatrix, Matrix newRelMatrix, double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1, double z2) {
      int mask = 0;

      if (child != null) {
         for (int i = 0; i < child.length; i++)
            if (child[i] != null) {

               Matrix tmp1 = new Matrix();
               tmp1.copy(oldRelMatrix);
               child[i].matrix.preMultiply(tmp1);

               Matrix tmp2 = new Matrix();
               tmp2.copy(newRelMatrix);
               child[i].matrix.preMultiply(tmp2);

               if ((pullMask & (1 << i)) != 0)
                  if (child[i].pull(tmp1, tmp2, x0, x1, x2, y0, y1, y2, z0, z1, z2) != 0)
                     mask |= (1 << i);
            }
         return mask;
      }

      if (refVertices == null) {
         refVertices = copyVertices(vertices);
         vCache = new double[nVertices()][3];
      }
      if (!modified) {
         copyVertices(refVertices, vertices);
         for (int i = 0; i < vCache.length; i++)
            vCache[i][0] = UNDEFINED;
         modified = true;
      }

      inverseOldRelMatrix.invert(oldRelMatrix);

      boolean pulled = false;

      double f, fx, fy, fz;
      for (int i = 0; i < nVertices(); i++) {
         double v[] = vertices[i];
         double w[] = vCache[i];
         if (w[0] == UNDEFINED)
            transform(v, oldRelMatrix, w);

         if ((fx = lump(w[0], x0, x1, x2)) >= 1)
            continue;
         if ((fy = lump(w[1], y0, y1, y2)) >= 1)
            continue;
         if ((fz = lump(w[2], z0, z1, z2)) >= 1)
            continue;

         pulled = true;

         if (fx < 0)
            fx = 0;
         if (fy < 0)
            fy = 0;
         if (fz < 0)
            fz = 0;
         f = fx * fx + fy * fy + fz * fz;

         if (f < 1) {
            if (f == 0)
               transform(v, newRelMatrix, w);
            else {
               transform(v, newRelMatrix, B);
               f = dropoff[(int) (D * f)] * pullWeight;
               w[0] += f * (B[0] - w[0]);
               w[1] += f * (B[1] - w[1]);
               w[2] += f * (B[2] - w[2]);
            }
            transform(w, inverseOldRelMatrix, v);
         }
      }
      if (pulled)
         computedMeshNormals = false;

      return pulled ? 1 : 0;
   }

   private double offset[] = { 0, 0, 0};

   private static final int D = 1000;
   private static double dropoff[] = new double[D];
   private static boolean initDropoff = computeDropoff();

   private static boolean computeDropoff() {
      for (int i = 0; i < D; i++) {
         double f = (double) i / D;
         dropoff[i] = .5 + .5 * Math.cos(Math.PI * Math.sqrt(f));
      }
      return true;
   }

   private double lump(double t, double a, double b, double c) {
      if (a == c)
         return 0;
      t = a == b || (t < b && c < b || t > b && c > b) ? (t - b) / (c - b) : (t - b) / (a - b);
      return t >= 1 ? 1 : t;
   }

   public static void transform(double src[], Matrix m, double dst[]) {
      dst[0] = m.get(0, 0) * src[0] + m.get(0, 1) * src[1] + m.get(0, 2) * src[2] + m.get(0, 3);
      dst[1] = m.get(1, 0) * src[0] + m.get(1, 1) * src[1] + m.get(1, 2) * src[2] + m.get(1, 3);
      dst[2] = m.get(2, 0) * src[0] + m.get(2, 1) * src[1] + m.get(2, 2) * src[2] + m.get(2, 3);

      double w = m.get(3, 0) * src[0] + m.get(3, 1) * src[1] + m.get(3, 2) * src[2] + m.get(3, 3);
      dst[0] /= w;
      dst[1] /= w;
      dst[2] /= w;
   }

   public Geometry bake(Geometry src) {
      if (src == null)
         return this;

      double[][] srcVertices = src.vertices;
      if (srcVertices == null)
         return this;

      int[][] srcFaces = src.faces;
      if (srcFaces == null)
         return this;

      if (vertices == null || nVertices() != srcVertices.length)
         vertices = new double[srcVertices.length][6];

      if (faces == null || faces.length != srcFaces.length) {
         faces = new int[srcFaces.length][];
         for (int f = 0 ; f < faces.length ; f++) {
            faces[f] = new int[srcFaces[f].length];
            for (int i = 0 ; i < faces[f].length ; i++)
               faces[f][i] = srcFaces[f][i];
         }
      }

      src.computeGlobalMatrix(m);

      for (int k = 0 ; k < nVertices() ; k++) {
         double sv[] = srcVertices[k];
         double v[] = vertices[k];
         transform(sv, m, v);
      }

      computeSurfaceNormals();

      return this;
   }

   public void computeGlobalMatrix(Matrix m) {
      m.identity();
      for (Geometry p = this ; p != null ; p = p.getParent())
         m.postMultiply(p.getMatrix());
   }

   //////////// HANDLING BICUBIC SPLINE PATCHES /////////////////

   /**
    Bezier basis matrix.
    */
   public static double[][] Bezier = { { -1, 3, -3, 1}, { 3, -6, 3, 0}, { -3, 3, 0, 0}, { 1, 0, 0, 0}};

   /**
    B-Spline basis matrix.
    */
   public static double[][] BSpline = { { -1 / 6., 1 / 2., -1 / 2., 1 / 6.}, { 1 / 2., -1., 1 / 2., 0},
         { -1 / 2., 0, 1 / 2., 0}, { 1 / 6., 2 / 3., 1 / 6., 0}};

   /**
    Catmull-Rom basis matrix.
    */
   public static double[][] CatmullRom = { { -0.5, 1.5, -1.5, 0.5}, { 1, -2.5, 2, -0.5}, { -0.5, 0, 0.5, 0},
         { 0, 1, 0, 0}};

   /**
    Hermite basis matrix.
    */
   public static double[][] Hermite = { { 2, -2, 1, 1}, { -3, 3, -2, -1}, { 0, 0, 1, 0}, { 1, 0, 0, 0}};

   /** 
    Inverse of the bezier basis matrix.
    */
   public static double[][] BezierInverse = { { 0, 0, 0, 1}, { 0, 0, 1. / 3, 1}, { 2, 5. / 3, 1. / 3, 1}, { 1, 1, 1, 1}};

   /**
    Subdivides one quad (I, J) of a bicubic mesh into a new m by n mesh.
    @param I row index of quad
    @param J column index of quad
    @param m number of rows of new patch
    @param n number of columns of the new patch
    @return the shape geometry
    */
   public Geometry subdivide(int I, int J, int m, int n) {
      double data[] = new double[48];
      for (int i = 0; i < 4; i++)
         for (int j = 0; j < 4; j++)
            for (int k = 0; k < 3; k++)
               data[12 * i + 3 * j + k] = vertices[(J + j) * (_m + 1) + (I + i)][k];
      faces[J * (_m - 2) + I] = null;
      Geometry p = (new Geometry()).patch(m, n, Geometry.CatmullRom, data);
      add(p);
      return p;
   }

   // CREATE A BICUBIC PATCH

   double Coefs[][][];

   /**
    Creates a bicubic m by n patch 
    @param m number of rows of the patch
    @param n number of columns in the patch
    @param basisMatrix 
    @param data
    @return the shape geometry
    */
   public Geometry patch(int m, int n, double basisMatrix[][], double data[]) {

      // TO MAKE NORMALS CORRECT AROUND THE EDGES, ALLOCATE EXTRA VERTICES...

      newRectangularMesh(m + 2, n + 2);

      // ...BUT DON'T ATTACH THOSE EXTRA VERTICES TO ANY FACES.

      faces = new int[m * n][4];
      for (int k = 1; k < n + 1; k++)
         for (int j = 1; j < m + 1; j++) {
            int f = (k - 1) * m + j - 1;
            int v = k * (m + 3) + j;
            faces[f][0] = v;
            faces[f][1] = v + 1;
            faces[f][2] = v + m + 3 + 1;
            faces[f][3] = v + m + 3;
         }

      // PUT KNOT DATA INTO PROPER ORDER FOR CONSTRUCTING X,Y,Z BICUBIC SPLINES

      double G[][][] = new double[3][4][4];
      for (int i = 0; i < 4; i++)
         for (int j = 0; j < 4; j++)
            for (int k = 0; k < 3; k++)
               G[k][i][j] = data[12 * i + 3 * j + k];

      // CONSTRUCT THE X,Y,Z COEFFICIENT MATRICES

      Coefs = new double[3][4][4];
      for (int k = 0; k < 3; k++)
         constructBicubicCoefficients(G[k], basisMatrix, Coefs[k]);

      // EVAL X,Y,Z BICUBIC SPLINES TO MAKE POLYGON MESH VERTICES

      int N = 0;
      for (int j = -1; j <= n + 1; j++) {
         double v = (double) j / n;
         for (int i = -1; i <= m + 1; i++) {
            double u = (double) i / m;
            for (int k = 0; k < 3; k++)
               vertices[N][k] = evalBicubic(Coefs[k], u, v);
            N++;
         }
      }

      // FORCE A COMPUTATION OF THE MESH VERTEX NORMALS

      computedMeshNormals = false;
      return this;
   }

   // BUILD ONE BICUBIC SPLINE FUNCTION COEFFICIENTS MATRIX

   void constructBicubicCoefficients(double[][] G, double[][] M, double[][] C) {
      double[][] tmp = new double[4][4]; //Matrix.tmp;

      for (int i = 0; i < 4; i++)
         // tmp = G x M_transpose
         for (int j = 0; j < 4; j++)
            for (int k = 0; k < 4; k++)
               tmp[i][j] += G[i][k] * M[j][k];

      for (int i = 0; i < 4; i++)
         // C = M x tmp
         for (int j = 0; j < 4; j++)
            for (int k = 0; k < 4; k++)
               C[i][j] += M[i][k] * tmp[k][j];
   }

   // EVALUATE A BICUBIC SPLINE FUNCTION AT ONE (u,v) VALUE

   double evalBicubic(double C[][], double u, double v) {
      return u
            * (u
                  * (u * (v * (v * (v * C[0][0] + C[0][1]) + C[0][2]) + C[0][3]) + (v
                        * (v * (v * C[1][0] + C[1][1]) + C[1][2]) + C[1][3])) + (v
                  * (v * (v * C[2][0] + C[2][1]) + C[2][2]) + C[2][3]))
            + (v * (v * (v * C[3][0] + C[3][1]) + C[3][2]) + C[3][3]);
   }

   double faceNormals[][];
   double vertNormals[][];

   public void computeSurfaceNormals() {
      computeSurfaceNormals(vertices);
   }

   public void computeSurfaceNormals(double vertices[][]) {

      if (faceNormals == null || faceNormals.length != faces.length)
         faceNormals = new double[faces.length][3];

      if (vertNormals == null || vertNormals.length != vertices.length)
         vertNormals = new double[vertices.length][4];

      // First compute normals of faces.

      for (int i = 0; i < nFaces() ; i++) {
         if (faces[i] != null) {
            for (int k = 0; k < 3; k++) {
               A[k] = vertices[faces[i][1]][k] - vertices[faces[i][2]][k];
               B[k] = vertices[faces[i][0]][k] - vertices[faces[i][1]][k];
            }
            Vec.cross(A, B, faceNormals[i]);
         }
      }

      // Then accumulate values + count of normals to compute avg. 

      for (int i = 0 ; i < vertNormals.length ; i++)
         for (int k = 0 ; k < 4 ; k++)
            vertNormals[i][k] = 0;

      for (int i = 0 ; i < nFaces() ; i++)
         if (faces[i] != null)
            for (int j = 0 ; j < faces[i].length && faces[i][j] >= 0 ; j++) {
               for (int k = 0; k < 3; k++)
                  vertNormals[faces[i][j]][k] += faceNormals[i][k];
               vertNormals[faces[i][j]][3]++;
            }

      for (int i = 0 ; i < vertNormals.length ; i++)
         for (int k = 0 ; k < 3 ; k++)
            vertices[i][3 + k] = vertNormals[i][k];

      if (meshRowSize > 0) {
         for (int m = 0 ; m <= _m ; m++)
            blendNormalsIfEqual(vertices, mn2i(m, 0), mn2i(m, _n));
         for (int n = 0 ; n <= _n ; n++)
            blendNormalsIfEqual(vertices, mn2i(0, n), mn2i(_m, n));
      }

      computedMeshNormals = true;
   }

   public int mn2i(int m, int n) { return m + n * (_m + 1); }

   void blendNormalsIfEqual(double[][] vertices, int i, int j) {
      double[] a = vertices[i];
      double[] b = vertices[j];
      for (int k = 0 ; k < 3 ; k++)
         if (Math.abs(a[k] - b[k]) > 0.01)
	    return;
      blendNormals(vertices, i, j);
   }

   public void blendNormals(double[][] vertices, int i, int j) {
      double[] a = vertices[i];
      double[] b = vertices[j];
      for (int k = 3 ; k < 6 ; k++)
         a[k] = b[k] = (a[k] + b[k]) / 2;
   }

   public void computeMeshNormals() {
      if (computedMeshNormals)
         return;

      if (meshRowSize < 0) {
         if (nVertices() > 0)
            computePolyhedronNormals();
         return;
      }

      int m = meshRowSize, n = nVertices() / (m + 1) - 1;

      double nn[] = new double[3];
      double A[] = new double[3], B[] = new double[3];
      int N = 0;
      if (vertices == null || n < 1 || m < 1)
         return;

      for (int j = 0; j <= n; j++)
         for (int i = 0; i <= m; i++) {

            if (i == 0)
               for (int k = 0; k < 3; k++)
                  A[k] = 1.5 * (V(i + 1, j)[k] - V(i, j)[k]) - .5 * (V(i + 2, j)[k] - V(i + 1, j)[k]);
            else if (i == m)
               for (int k = 0; k < 3; k++)
                  A[k] = 1.5 * (V(i, j)[k] - V(i - 1, j)[k]) - .5 * (V(i - 1, j)[k] - V(i - 2, j)[k]);
            else
               for (int k = 0; k < 3; k++)
                  A[k] = .5 * (V(i + 1, j)[k] - V(i - 1, j)[k]);

            if (j == 0)
               for (int k = 0; k < 3; k++)
                  B[k] = 1.5 * (V(i, j + 1)[k] - V(i, j)[k]) - .5 * (V(i, j + 2)[k] - V(i, j + 1)[k]);
            else if (j == n)
               for (int k = 0; k < 3; k++)
                  B[k] = 1.5 * (V(i, j)[k] - V(i, j - 1)[k]) - .5 * (V(i, j - 1)[k] - V(i, j - 2)[k]);
            else
               for (int k = 0; k < 3; k++)
                  B[k] = .5 * (V(i, j + 1)[k] - V(i, j - 1)[k]);

            Vec.cross(A, B, nn);
            Vec.normalize(nn);
            for (int k = 0; k < 3; k++)
               vertices[N][k + 3] = nn[k];
            N++;
         }

      computedMeshNormals = true;
   }

   public void wrapMeshNormals(boolean wrapCols, boolean wrapRows) {
      if (vertices != null) {
         if (wrapCols)
            for (int i = 0 ; i < nVertices() ; i += _m + 1)
               blendNormals(i, i + _m);
         if (wrapRows)
            for (int i = 0 ; i < _m + 1 ; i++)
               blendNormals(i, i + nVertices() - (_m + 1));
      }
      for (int n = 0 ; child(n) != null ; n++)
         child(n).wrapMeshNormals(wrapCols, wrapRows);
   }

   void blendNormals(int i, int j) {
      for (int k = 0 ; k < 3 ; k++)
         normal[k] = vertices[i][3 + k] + vertices[j][3 + k];
      Vec.normalize(normal);
      for (int k = 0 ; k < 3 ; k++)
         vertices[i][3 + k] = vertices[j][3 + k] = normal[k];
   }

   double normal[] = new double[3];
 
   Matrix matrixSave = new Matrix();

   public Geometry makeWireframe(double radius) {
      Geometry wireframe = new Geometry();
      matrixSave.copy(getMatrix());
      getMatrix().identity();

      buildWireframe(wireframe, radius);

      getMatrix().copy(matrixSave);
      wireframe.getMatrix().copy(matrixSave);
      return wireframe;
   }

   void buildWireframe(Geometry wireframe, double radius) {
      if (nVertices() > 0)
         (new Geometry()).bake(this).addToWireframe(wireframe, radius);
      for (int i = 0 ; child(i) != null ; i++)
         child(i).buildWireframe(wireframe, radius);
   }

   void addToWireframe(Geometry wireframe, double radius) {

      int edges[] = findEdges();

      for (int n = 0 ; n < edges.length ; n++) {
         int i = edges[n] & 0xffff;
         int j = edges[n] >> 16 & 0xffff;
         Matrix m = wireframe.add().tube(8).getMatrix();
         computeEdgeMatrix(vertices[i], vertices[j], radius, m);
      }

      int nve = 0, ve[] = new int[edges.length];

      for (int n = 0 ; n < edges.length ; n++) {
         for (int b = 0 ; b < 2 ; b++) {
            int i = edges[n] >> (b << 4) & 0xffff;

            int k = 0;
            for ( ; k < nve ; k++)
               if (compareXYZ(ve[k], i) == 0)
                  break;

            if (k == nve) {
               ve[nve++] = i;
               Matrix m = wireframe.add().sphere(2).getMatrix();
               double v[] = vertices[i];
               m.translate(v[0], v[1], v[2]);
               m.scale(radius);
            }
         }
      }
   }

   public int[] findEdges() {
      int count = 0;
      for (int f = 0 ; f < nFaces() ; f++)
         count += faces[f].length;
      int tmp[] = new int[count];

      int nEdges = 0;
      for (int f = 0 ; f < nFaces() ; f++) {
         int face[] = faces[f];
         for (int i = 0 ; i < face.length ; i++) {
            int j = (i + 1) % face.length;

            int a = face[i];
            int b = face[j];
            int edge = a | b << 16;

            int k = 0;
            for ( ; k < nEdges ; k++) {
               int ta = tmp[k] & 0xffff;
               int tb = tmp[k] >> 16 & 0xffff;
               if ( compareXYZ(a, ta) == 0 && compareXYZ(b, tb) == 0 ||
                    compareXYZ(a, tb) == 0 && compareXYZ(b, ta) == 0 )
                  break;
            }

            if (k == nEdges) {
               tmp[nEdges++] = edge;
            }
         }
      }

      int edges[] = new int[nEdges];
      for (int k = 0 ; k < nEdges ; k++)
         edges[k] = tmp[k];

      return edges;
   }


   int compareXYZ(int i, int j) {
      return compareXYZ(vertices[i], vertices[j]);
   }

   int compareXYZ(double a[], double b[]) {
      return a[0] < b[0] ? -1 :
             a[0] > b[0] ?  1 : a[1] < b[1] ? -1 :
                                a[1] > b[1] ?  1 : a[2] < b[2] ? -1 :
                                                   a[2] > b[2] ?  1 : 0;
   }

   public void computeEdgeMatrix(double a[], double b[], double radius, Matrix m) {
      m.identity();
      m.translate((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2);

      for (int j = 0 ; j < 3 ; j++) {
         Y[j] = j == 1 ? 1 : .001 * Math.random();
         Z[j] = b[j] - a[j];
      }

      Vec.normalize(Z);
      Vec.cross(Y, Z, X);
      Vec.normalize(X);
      Vec.cross(Z, X, Y);

      double len = Math.sqrt( (b[0] - a[0]) * (b[0] - a[0]) +
                              (b[1] - a[1]) * (b[1] - a[1]) +
                              (b[2] - a[2]) * (b[2] - a[2]) );

      for (int j = 0 ; j < 3 ; j++) {
         m.set(j, 0, X[j] * radius);
         m.set(j, 1, Y[j] * radius);
         m.set(j, 2, Z[j] * len / 2);
      }
   }

   double X[] = new double[3];
   double Y[] = new double[3];
   double Z[] = new double[3];


   // SUPPORT FOR MULTIPLE BLENDED MATERIALS

   public Material[] getMaterials() {
      return materials;
   }

   public double[][] getMaterialWeights() {
      return materialWeights;
   }

   public void setMaterial(int m, Material material) {
      materials[m] = material;
   }

   public void setMaterialWeight(int m, int vi, double weight) {
      int nv = nVertices();
      if (vi >= nv)
         return;
      if (materialWeights[m] == null)
         materialWeights[m] = new double[nv];
      if (materialWeights[m].length < nv) {
         double mw[] = new double[nv];
         for (int n = 0 ; n < materialWeights[m].length ; n++)
            mw[n] = materialWeights[m][n];
         materialWeights[m] = mw;
      }
      materialWeights[m][vi] = weight;
   }

   private Material[] materials = new Material[32];
   private double[][] materialWeights = new double[32][];
   private String textureFileName = "";
   private String text = "";
   private double anchor = -100000.0;
}

