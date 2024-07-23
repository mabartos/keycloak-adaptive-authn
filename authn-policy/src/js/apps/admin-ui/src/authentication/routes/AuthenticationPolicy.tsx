import {lazy} from "react";
import type {Path} from "react-router-dom";
import {generateEncodedPath} from "../../utils/generateEncodedPath";
import type {AppRouteObject} from "../../routes";

export type AuthenticationPolicyParams = {
    realm: string;
    id: string;
};

const AuthenticationPolicyDetails = lazy(() => import("../AuthenticationPolicyDetails"));

export const AuthenticationPolicyRoute: AppRouteObject = {
    path: "/:realm/authentication/authn-policy/:id/",
    element: <AuthenticationPolicyDetails/>,
    breadcrumb: (t) => t("authPolicyDetails"),
    handle: {
        access: "view-authorization",
    },
};

export const toAuthenticationPolicy = (params: AuthenticationPolicyParams): Partial<Path> => {
    return {
        pathname: generateEncodedPath(AuthenticationPolicyRoute.path, params),
    };
};
