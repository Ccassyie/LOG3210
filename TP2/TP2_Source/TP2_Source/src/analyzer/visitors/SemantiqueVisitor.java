package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import javax.lang.model.element.VariableElement;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created: 19-01-10
 * Last Changed: 19-01-25
 * Author: Félix Brunet
 * <p>
 * Description: Ce visiteur explorer l'AST est renvois des erreur lorqu'une erreur sémantique est détecté.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    private HashMap<String, VarType> SymbolTable = new HashMap<>(); // mapping variable -> type

    // variable pour les metrics
    private int VAR = 0;
    private int WHILE = 0;
    private int IF = 0;
    private int FOR = 0;
    private int OP = 0;
    private boolean error = false;

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    Le Visiteur doit lancer des erreurs lorsqu'un situation arrive.

    regardez l'énoncé ou les tests pour voir le message à afficher et dans quelle situation.
    Lorsque vous voulez afficher une erreur, utilisez la méthode print implémentée ci-dessous.
    Tous vos tests doivent passer!!

     */

    @Override
    public Object visit(SimpleNode node, Object data) {
//        node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, data);
        print(String.format("{VAR:%d, WHILE:%d, IF:%d, FOR:%d, OP:%d}", VAR, WHILE, IF, FOR, OP));
        return data;
    }

    /*
    Appelez cette méthode pour afficher vos erreurs.
     */
    private void print(final String msg) {
        if (!error) {
            m_writer.print(msg);
            error = true;
        }
    }

    /*
    Ici se retrouve les noeuds servant à déclarer une variable.
    Certaines doivent enregistrer les variables avec leur type dans la table symbolique.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTNormalDeclaration node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if(SymbolTable.containsKey(varName)){
            throw new SemantiqueError("Invalid declaration... variable " + varName + " already exists");
        } // Il manque la condition pour afficher l'erreur Invalid use of undefined b et lire les listnum et listbool
        else{
            SymbolTable.put(varName, node.getValue().equals("num")? VarType.num:VarType.bool);
            this.VAR++;
        }
        return data;
    }

    @Override
    public Object visit(ASTListDeclaration node, Object data) {
        node.childrenAccept(this, data);
        this.VAR++;
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    /*
     * Il faut vérifier que le type déclaré à gauche soit compatible avec la liste utilisée à droite. N'oubliez pas
     * de vérifier que les variables existent.
     */

    @Override
    public Object visit(ASTForEachStmt node, Object data) {
        // node.childrenAccept(this, data);
        DataStruct exprType = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, exprType);

        //VarType type = ((DataStruct)data).type;
        if(exprType.type != VarType.bool){
            throw new SemantiqueError("Array type "+exprType.type+" is incompatible with declared variable of type num...");
            //throw new SemantiqueError("Array type"+ exprType.type+ "is incompatible with declared variable of type "+exprType.type+"...");
        }
        FOR++;
        return data;
    }

    private void callChildenCond(SimpleNode node) {

    }

    /*
    les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
    On doit aussi compter les conditions dans les variables IF et WHILE
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        DataStruct exprType = new DataStruct();
        if(exprType.type != VarType.bool)
            throw new SemantiqueError("Invalid type in condition.");
        else
            this.IF++;
        return data;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        DataStruct exprType = new DataStruct();
        if(exprType.type != VarType.bool)
            throw new SemantiqueError("Invalid type in condition");
        else
            this.WHILE++;
        return data;
    }

    /*
    On doit vérifier que le type de la variable est compatible avec celui de l'expression.
    La variable doit etre déclarée.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        VarType type = SymbolTable.get(((ASTIdentifier) node.jjtGetChild(0)).getValue());
        data = new DataStruct();
        node.jjtGetChild(1).jjtAccept(this, data);
        if(type != ((DataStruct)data).type)
            throw new SemantiqueError("Invalid type in assignation of Identifier " + ((ASTIdentifier) node.jjtGetChild(0)).getValue() + "... was expecting " + type + " but got " + ((DataStruct) data).type);
        return data;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        //Il est normal que tous les noeuds jusqu'à expr retourne un type.
        node.childrenAccept(this, data);
        return null;
    }
    //TODO
    @Override
    public Object visit(ASTCompExpr node, Object data) {
        /*attention, ce noeud est plus complexe que les autres.
        si il n'a qu'un seul enfant, le noeud a pour type le type de son enfant.

        si il a plus d'un enfant, alors ils s'agit d'une comparaison. il a donc pour type "Bool".

        de plus, il n'est pas acceptable de faire des comparaisons de booleen avec les opérateur < > <= >=.
        les opérateurs == et != peuvent être utilisé pour les nombres et les booléens, mais il faut que le type soit le même
        des deux côté de l'égalité/l'inégalité.
        */

        ArrayList<VarType> childrenTypes = new ArrayList<>();
        if (node.getValue() != null){
            DataStruct datastruct = new DataStruct();

            node.jjtGetChild(0).jjtAccept(this, (DataStruct)data);
            node.jjtGetChild(1).jjtAccept(this, datastruct);

            if(((DataStruct)data).type==VarType.bool && datastruct.type==VarType.bool){
                if(node.getValue().equals("!=")|| node.getValue().equals("==") || node.getValue().equals(">") || node.getValue().equals("<") ||node.getValue().equals("<=") || node.getValue().equals(">=")){
                    throw new SemantiqueError("Invalid type in expression");
                }
            }

            if(((DataStruct)data).type!=datastruct.type){
                throw new SemantiqueError("Invalid type in expression");
            }

            ((DataStruct) data).type = VarType.bool;
            this.OP++;
        }
        else {
            node.childrenAccept(this, data);
        }
        return data;
    }

    private void callChildren(SimpleNode node, Object data, VarType validType) {

    }

    /*
    opérateur binaire
    si il n'y a qu'un enfant, aucune vérification à faire.
    par exemple, un AddExpr peut retourné le type "Bool" à condition de n'avoir qu'un seul enfant.
     */
    //TODO
    @Override
    public Object visit(ASTAddExpr node, Object data) {
        if(!node.getOps().isEmpty()){
            for(int i=0; i<node.jjtGetNumChildren();i++){
                DataStruct datastruct = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this,datastruct);
                if(datastruct.type!=VarType.num){
                    throw new SemantiqueError("Invalid type in expression.");
                }
            }
            ((DataStruct)data).type=VarType.num;
            this.OP++;
        }
        else
            node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        if(!node.getOps().isEmpty()){
            for(int i=0; i<node.jjtGetNumChildren();i++){
                DataStruct datastruct = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this,datastruct);
                if(datastruct.type!=VarType.num){
                    throw new SemantiqueError("Invalid type in expression.");
                }
            }
            ((DataStruct)data).type=VarType.num;
            this.OP++;
        }
        else
            node.childrenAccept(this, data);
        return data;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        if(!node.getOps().isEmpty()){
            for(int i=0; i<node.jjtGetNumChildren();i++){
                DataStruct datastruct = new DataStruct();
                node.jjtGetChild(i).jjtAccept(this,datastruct);
                if(datastruct.type!=VarType.bool){
                    throw new SemantiqueError("Invalid type in expression.");
                }
            }
            ((DataStruct)data).type=VarType.bool;
            this.OP++;
        }
        else
            node.childrenAccept(this, data);
        return data;
    }

    /*
    opérateur unaire
    les opérateur unaire ont toujours un seul enfant.

    Cependant, ASTNotExpr et ASTUnaExpr ont la fonction "getOps()" qui retourne un vecteur contenant l'image (représentation str)
    de chaque token associé au noeud.

    Il est utile de vérifier la longueur de ce vecteur pour savoir si une opérande est présente.

    si il n'y a pas d'opérande, ne rien faire.
    si il y a une (ou plus) opérande, ils faut vérifier le type.

    */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        node.childrenAccept(this, data);
        if(!node.getOps().isEmpty()){
            if(((DataStruct)data).type != VarType.bool) {
                throw new SemantiqueError("Invalid type in expression");
            }
            this.OP++;
        }
        return data;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        node.childrenAccept(this, data);
        if(!node.getOps().isEmpty()){
            if(((DataStruct)data).type == VarType.bool) {
                throw new SemantiqueError("Invalid type in expression");
            }
            this.OP++;
        }
        return data;
    }

    /*
    les noeud ASTIdentifier aillant comme parent "GenValue" doivent vérifier leur type et vérifier leur existence.

    Ont peut envoyé une information a un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.childrenAccept(this, data);
        return data;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        ((DataStruct) data).type = VarType.bool;
        return data;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        if (node.jjtGetParent() instanceof ASTGenValue) {
            ((DataStruct) data).type = SymbolTable.get(node.getValue());
        }
        return data;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        ((DataStruct) data).type = VarType.num;
        return data;
    }


    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        bool,
        num,
        listnum,
        listbool
    }

    private class DataStruct {
        public VarType type;

        public DataStruct() {
        }

        public DataStruct(VarType p_type) {
            type = p_type;
        }
    }
}
