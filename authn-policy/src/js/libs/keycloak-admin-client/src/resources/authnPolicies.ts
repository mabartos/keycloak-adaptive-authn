import Resource from "./resource.js";
import type {KeycloakAdminClient} from "../client.js";
import type AuthenticationFlowRepresentation from "../defs/authenticationFlowRepresentation.js";
import type AuthenticationExecutionInfoRepresentation from "../defs/authenticationExecutionInfoRepresentation.js";

export class AuthenticationPolicies extends Resource<{ realm?: string }> {

    public getPolicies = this.makeRequest<{}, AuthenticationExecutionInfoRepresentation[]>({
        method: "GET",
        path: "/authn-policies",
    });

    public getPolicy = this.makeRequest<
        { id: string },
        AuthenticationFlowRepresentation
    >({
        method: "GET",
        path: "/authn-policies/{id}",
        urlParamKeys: ["id"],
    });

    public getParentPolicy = this.makeRequest<
        { flowId: string },
        AuthenticationFlowRepresentation
    >({
        method: "GET",
        path: "/authn-policies/parent"
    });

    public createPolicy = this.makeRequest<
        AuthenticationFlowRepresentation,
        AuthenticationFlowRepresentation
    >({
        method: "POST",
        path: "/authn-policies",
        returnResourceIdInLocationHeader: {field: "id"},
    });

    public deletePolicy = this.makeRequest<{ flowId: string }>({
        method: "DELETE",
        path: "/authn-policies/{flowId}",
        urlParamKeys: ["flowId"],
    });

    public updatePolicy = this.makeUpdateRequest<
        { flowId: string },
        AuthenticationFlowRepresentation
    >({
        method: "PUT",
        path: "/authn-policies/{flowId}",
        urlParamKeys: ["flowId"],
    });

    constructor(client: KeycloakAdminClient) {
        super(client, {
            path: "/realms/{realm}",
            getUrlParams: () => ({
                realm: client.realmName,
            }),
            getBaseUrl: () => client.baseUrl,
        });
    }
}
