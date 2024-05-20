import {useState} from "react";
import {useTranslation} from "react-i18next";
import {
    Button,
    Flex,
    FlexItem,
    Title,
    TitleSizes,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation
    from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {ListEmptyState} from "../components/list-empty-state/ListEmptyState";

import "./empty-execution-state.css";
import {AddSubPolicyModal, Policy} from "./components/modals/AddSubPolicyModal";
import type {
    AuthenticationProviderRepresentation
} from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {AddStepModal} from "./components/modals/AddStepModal";

const SECTIONS = ["addExecution", "addCondition", "addAuthnPolicy"] as const;
type SectionType = (typeof SECTIONS)[number] | undefined;

type EmptyAuthenticationPolicyProps = {
    policy: AuthenticationFlowRepresentation;
    onAddExecution: (type: AuthenticationProviderRepresentation) => void;
    onAddCondition: (type: AuthenticationProviderRepresentation) => void;
    onAddSubPolicy: (flow: Policy) => void;
    isParentPolicy: boolean;
};

export const EmptyAuthenticationPolicy = ({
                                              policy,
                                              onAddExecution,
                                              onAddCondition,
                                              onAddSubPolicy,
                                              isParentPolicy
                                          }: EmptyAuthenticationPolicyProps) => {
    const {t} = useTranslation();
    const [show, setShow] = useState<SectionType>();
    const getSections = (): SectionType[] => isParentPolicy ? ["addAuthnPolicy"] : ["addExecution", "addCondition", "addAuthnPolicy"];
    const getTitleMessage = () => isParentPolicy ? "noAuthenticationPolicies" : "emptyAuthenticationPolicy";
    const getDescriptionMessage = () => isParentPolicy ? "noAuthenticationPoliciesInstructions" : "emptyAuthenticationPolicyInstructions";

    const getPolicyMessage = () => isParentPolicy ? "addAuthnPolicy" : "addAuthnSubPolicy";
    const getPolicyTitleMessage = () => isParentPolicy ? "addAuthnPolicyTitle" : "addAuthnSubPolicyTitle";
    const getPolicyDescriptionMessage = () => isParentPolicy ? "addAuthnPolicyDescription" : "addAuthnSubPolicyDescription";

    return (
        <>
            {show === "addExecution" && (
                <AddStepModal
                    name={policy.alias!}
                    type={"basic"}
                    onSelect={(type) => {
                        if (type) {
                            onAddExecution(type);
                        }
                        setShow(undefined);
                    }}
                />
            )}
            {show === "addCondition" && (
                <AddStepModal
                    name={policy.alias!}
                    type={"condition"}
                    onSelect={(type) => {
                        if (type) {
                            onAddCondition(type);
                        }
                        setShow(undefined);
                    }}
                />
            )}
            {show === "addAuthnPolicy" && (
                <AddSubPolicyModal
                    name={policy.alias!}
                    onCancel={() => setShow(undefined)}
                    onConfirm={(newFlow) => {
                        onAddSubPolicy(newFlow);
                        setShow(undefined);
                    }}
                />
            )}
            <ListEmptyState
                message={t(getTitleMessage())}
                instructions={t(getDescriptionMessage())}
            />

            <div className="keycloak__empty-execution-state__block">
                {getSections().map((section) => (
                    <Flex key={section} className="keycloak__empty-execution-state__help">
                        <FlexItem flex={{default: "flex_1"}}>
                            <Title headingLevel="h2" size={TitleSizes.md}>
                                {section === "addAuthnPolicy" ? t(getPolicyTitleMessage()) : t(`${section}Title`)}
                            </Title>
                            <p>{section === "addAuthnPolicy" ? t(getPolicyDescriptionMessage()) : t(`${section}Description`)}</p>
                        </FlexItem>
                        <Flex alignSelf={{default: "alignSelfCenter"}}>
                            <FlexItem>
                                <Button
                                    data-testid={section}
                                    variant="tertiary"
                                    onClick={() => setShow(section)}
                                >
                                    {section === "addAuthnPolicy" ? t(getPolicyMessage()) : t(section!)}
                                </Button>
                            </FlexItem>
                        </Flex>
                    </Flex>
                ))}
            </div>
        </>
    );
};
