import type { AppRouteObject } from "../routes";
import {
  AuthenticationRoute,
  AuthenticationRouteWithTab,
} from "./routes/Authentication";
import { CreateFlowRoute } from "./routes/CreateFlow";
import { FlowRoute, FlowWithBuiltInRoute } from "./routes/Flow";
import { CreateAuthnPolicyRoute} from "./routes/CreateAuthenticationPolicy";
import {AuthenticationPolicyRoute} from "./routes/AuthenticationPolicy";

const routes: AppRouteObject[] = [
  AuthenticationRoute,
  AuthenticationRouteWithTab,
  CreateFlowRoute,
  CreateAuthnPolicyRoute,
  AuthenticationPolicyRoute,
  FlowRoute,
  FlowWithBuiltInRoute,
];

export default routes;
