package org.batfish.question.ip_set_expr;

import java.util.Set;

import org.batfish.question.Environment;
import org.batfish.representation.Ip;

public final class VarIpSetExpr extends BaseIpSetExpr {

   private final String _var;

   public VarIpSetExpr(String var) {
      _var = var;
   }

   @Override
   public Set<Ip> evaluate(Environment environment) {
      return environment.getIpSets().get(_var);
   }

}
