import type AuthenticationFlowRepresentation
    from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {
    ActionGroup,
    AlertVariant,
    Button,
    PageSection,
} from "@patternfly/react-core";
import {FormProvider, useForm} from "react-hook-form";
import {useTranslation} from "react-i18next";
import {Link, useNavigate} from "react-router-dom";

import {SelectControl} from "ui-shared";
import {adminClient} from "../../admin-client";
import {useAlerts} from "../../components/alert/Alerts";
import {FormAccess} from "../../components/form/FormAccess";
import {ViewHeader} from "../../components/view-header/ViewHeader";
import {useRealm} from "../../context/realm-context/RealmContext";
import {toAuthentication} from "../routes/Authentication";
import {toFlow} from "../routes/Flow";
import {NameDescription} from "./NameDescription";

export default function CreateAuthenticationPolicy() {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {realm} = useRealm();
    const {addAlert} = useAlerts();
    const form = useForm<AuthenticationFlowRepresentation>();
    const {handleSubmit} = form;

    const onSubmit = async (formValues: AuthenticationFlowRepresentation) => {
        const flow = {
            ...formValues,
            alias: "POLICY -" + formValues.alias, // Just for now add prefix POLICY
            builtIn: false,
            topLevel: true,
            providerId: "basic-flow"
        };

        try {
            const {id} =
                await adminClient.authenticationManagement.createFlow(flow);
            addAlert(t("authnPolicyCreatedSuccess"), AlertVariant.success);
            navigate(
                toFlow({
                    realm,
                    id: id!,
                    usedBy: "notInUse",
                }),
            );
        } catch (error: any) {
            addAlert(
                t("authnPolicyCreateError", {
                    error: error.response?.data?.errorMessage || error,
                }),
                AlertVariant.danger,
            );
        }
    };

    return (
        <>
            <ViewHeader titleKey="createAuthnPolicy" subKey="authenticationCreateFlowHelp"/>
            <PageSection variant="light">
                <FormProvider {...form}>
                    <FormAccess
                        isHorizontal
                        role="manage-authorization"
                        onSubmit={handleSubmit(onSubmit)}
                    >
                        <NameDescription/>
                        <ActionGroup>
                            <Button data-testid="create" type="submit">
                                {t("create")}
                            </Button>
                            <Button
                                data-testid="cancel"
                                variant="link"
                                component={(props) => (
                                    <Link {...props} to={toAuthentication({realm, tab: "authnPolicies"})}></Link>
                                )}
                            >
                                {t("cancel")}
                            </Button>
                        </ActionGroup>
                    </FormAccess>
                </FormProvider>
            </PageSection>
        </>
    );
}
