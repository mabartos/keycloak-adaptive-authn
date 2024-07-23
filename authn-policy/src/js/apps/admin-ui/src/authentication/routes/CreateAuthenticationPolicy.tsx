import {lazy} from "react";
import type {Path} from "react-router-dom";
import {generateEncodedPath} from "../../utils/generateEncodedPath";
import type {AppRouteObject} from "../../routes";

export type CreateAuthnPolicyParams = { realm: string };

const CreateAuthenticationPolicy = lazy(() => import("../form/CreateAuthenticationPolicy"));

export const CreateAuthnPolicyRoute: AppRouteObject = {
    path: "/:realm/authentication/authn-policy/create", // TODO EDIT
    element: <CreateAuthenticationPolicy/>,
    breadcrumb: (t) => t("createAuthnPolicy"),
    handle: {
        access: "manage-authorization",
    },
};

export const toCreateAuthenticationPolicy = (params: CreateAuthnPolicyParams): Partial<Path> => ({
    pathname: generateEncodedPath(CreateAuthnPolicyRoute.path, params),
});
